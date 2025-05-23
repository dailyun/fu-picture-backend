package com.dali.fupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dali.fupicturebackend.exception.BusinessException;
import com.dali.fupicturebackend.exception.ErrorCode;
import com.dali.fupicturebackend.exception.ThrowUtils;
import com.dali.fupicturebackend.mapper.SpaceMapper;
import com.dali.fupicturebackend.model.dto.space.SpaceAddRequest;
import com.dali.fupicturebackend.model.dto.space.SpaceDeleteRequest;
import com.dali.fupicturebackend.model.dto.space.SpaceQueryRequest;
import com.dali.fupicturebackend.model.entity.Picture;
import com.dali.fupicturebackend.model.entity.Space;
import com.dali.fupicturebackend.model.entity.SpaceUser;
import com.dali.fupicturebackend.model.entity.User;
import com.dali.fupicturebackend.model.enums.SpaceLevelEnum;
import com.dali.fupicturebackend.model.enums.SpaceRoleEnum;
import com.dali.fupicturebackend.model.enums.SpaceTypeEnum;
import com.dali.fupicturebackend.model.vo.SpaceVO;
import com.dali.fupicturebackend.service.PictureService;
import com.dali.fupicturebackend.service.SpaceService;
import com.dali.fupicturebackend.service.SpaceUserService;
import com.dali.fupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    private final TransactionTemplate transactionTemplate;
    private final UserService userService;
    private final PictureService pictureService;
    private final SpaceUserService spaceUserService;


    // 添加 @Lazy 注解延迟加载

    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    public SpaceServiceImpl(TransactionTemplate transactionTemplate,
                            UserService userService,
                            @Lazy PictureService pictureService,
                            @Lazy SpaceUserService spaceUserService) {
        this.transactionTemplate = transactionTemplate;
        this.userService = userService;
        this.pictureService = pictureService;
        this.spaceUserService = spaceUserService;

    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {

        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        space.setUserId(loginUser.getId());
        // 填充数据
        this.fillSpaceBySpaceLevel(space);

        validSpace(space, true);

        // 权限校验
        SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (!userService.isAdmin(loginUser) && levelEnum != SpaceLevelEnum.COMMON) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建此级别空间");
        }

        // 单例锁保证唯一性
        Long userId = loginUser.getId();
        synchronized (lockMap.computeIfAbsent(userId, k -> new Object())) {
            try {
                 Long newSpaceId = transactionTemplate.execute(status -> {
                    if (!userService.isAdmin(loginUser)) {
                        boolean exists = this.lambdaQuery()
                                .eq(Space::getUserId, userId)
                                .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                                .exists();
                        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
                    }
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                     // 如果是团队空间，关联新增团队成员记录
                     if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                         SpaceUser spaceUser = new SpaceUser();
                         spaceUser.setSpaceId(space.getId());
                         spaceUser.setUserId(userId);
                         spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                         result = spaceUserService.save(spaceUser);
                         ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                     }
                    return space.getId();
                });
//                return transactionTemplate.execute(status -> {
//                    if (isExistSpaceByUserId(userId)) {
//                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户已存在私有空间");
//                    }
//                    save(space);
//                    return space.getId();
//                });
            } finally {
                lockMap.remove(userId);
            }
        }
        return space.getId();
    }

    @Override
    public boolean deleteSpace(SpaceDeleteRequest request, User loginUser) {
        Long spaceId = request.getSpaceId();
        validDeleteSpace(spaceId, loginUser);

        // 删除空间
        // 处理关联图片
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // 删除空间
            if (!removeById(spaceId)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间删除失败");
            }

            // 处理关联图片
            List<Picture> pictures = pictureService.getListBySpaceId(spaceId, loginUser.getId());
            if (CollUtil.isNotEmpty(pictures)) {
                pictureService.removeBatchByIds(pictures.stream().map(Picture::getId).collect(Collectors.toList()));
                pictures.forEach(pictureService::clearPictureFile);
            }
            return true;
        }));
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO vo = SpaceVO.objToVo(space);
        if (vo.getUserId() != null) {
            User user = userService.getById(vo.getUserId());
            vo.setUser(userService.getUserVO(user));
        }
        return vo;
    }

    @Override
    public org.springframework.data.domain.Page<SpaceVO> getSpaceVOPage(org.springframework.data.domain.Page<Space> spacePage, HttpServletRequest request) {
        return null;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> page, HttpServletRequest request) {
        // 1. 获取分页数据
        List<Space> spaces = page.getRecords();
        if (CollUtil.isEmpty(spaces)) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 2. 批量查询用户信息（使用 MyBatis-Plus 的 listByIds）
        Set<Long> userIds = spaces.stream()
                .map(Space::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = CollectionUtils.isEmpty(userIds) ?
                new HashMap<>() :
                userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 3. 转换 VO 并填充用户信息
        List<SpaceVO> vos = spaces.stream().map(space -> {
            SpaceVO vo = SpaceVO.objToVo(space);
            User user = userMap.get(space.getUserId());
            vo.setUser(userService.getUserVO(user));
            return vo;
        }).collect(Collectors.toList());

        // 4. 创建新的 MyBatis-Plus 分页对象
        Page<SpaceVO> voPage = new Page<>();
        BeanUtils.copyProperties(page, voPage); // 复制分页参数
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest request) {
        return new QueryWrapper<Space>()
                .eq(ObjUtil.isNotEmpty(request.getId()), "id", request.getId())
                .eq(ObjUtil.isNotEmpty(request.getUserId()), "userId", request.getUserId())
                .like(StringUtils.isNotBlank(request.getSpaceName()), "spaceName", request.getSpaceName())
                .eq(ObjUtil.isNotEmpty(request.getSpaceType()), "spaceType", request.getSpaceType())
                .eq(ObjUtil.isNotEmpty(request.getSpaceLevel()), "spaceLevel", request.getSpaceLevel())
                .orderBy(StringUtils.isNotBlank(request.getSortField()),
                        "ascend".equals(request.getSortOrder()),
                        request.getSortField());
    }

    @Override
    public void validSpace(Space space, boolean isAdd) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        String name = space.getSpaceName();
        Integer level = space.getSpaceLevel();

        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        if (isAdd) {
            if (spaceTypeEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型非法");
            }
        }

        if ( spaceType != null && spaceTypeEnum ==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }

        if (isAdd) {
            ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(level == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
        }

        if (StringUtils.isNotBlank(name) && name.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        ThrowUtils.throwIf(level != null && SpaceLevelEnum.getEnumByValue(level) == null,
                ErrorCode.PARAMS_ERROR, "空间级别非法");
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (levelEnum == null) return;

        if (space.getMaxSize() == null) {
            space.setMaxSize(levelEnum.getMaxSize());
        }
        if (space.getMaxCount() == null) {
            space.setMaxCount(levelEnum.getMaxCount());
        }
    }

    private Space buildInsertSpace(SpaceAddRequest request, User loginUser) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        space.setUserId(loginUser.getId());
        fillSpaceBySpaceLevel(space);
        return space;
    }

    private boolean isExistSpaceByUserId(Long userId) {
        return count(new QueryWrapper<Space>().eq("userId", userId)) > 0;
    }

    private void validDeleteSpace(Long spaceId, User loginUser) {
        Space space = getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR, "无权删除该空间");
    }
}