package com.dali.fupicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dali.fupicturebackend.annotation.AuthCheck;
import com.dali.fupicturebackend.common.BaseResponse;
import com.dali.fupicturebackend.common.ResultUtils;
import com.dali.fupicturebackend.constant.UserConstant;
import com.dali.fupicturebackend.exception.BusinessException;
import com.dali.fupicturebackend.exception.ErrorCode;
import com.dali.fupicturebackend.exception.ThrowUtils;
import com.dali.fupicturebackend.manager.auth.SpaceUserAuthManager;
import com.dali.fupicturebackend.model.dto.space.*;
import com.dali.fupicturebackend.model.entity.Space;
import com.dali.fupicturebackend.model.entity.User;
import com.dali.fupicturebackend.model.enums.SpaceLevelEnum;
import com.dali.fupicturebackend.model.vo.SpaceLevel;
import com.dali.fupicturebackend.model.vo.SpaceVO;
import com.dali.fupicturebackend.service.SpaceService;
import com.dali.fupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;
@Slf4j
@RequestMapping("/space")
@RestController
public class SpaceController {

    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;


    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        Long spaceId= spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody SpaceDeleteRequest spaceDeleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceDeleteRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!spaceService.deleteSpace(spaceDeleteRequest, loginUser), ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(true);
    }


    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam("id") long id,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest req) {
        long current = req.getCurrent();
        long size = req.getPageSize();
        Page<Space> page = new Page<>(current, size);
        QueryWrapper<Space> queryWrapper = spaceService.getQueryWrapper(req);
        Page<Space> spacePage = spaceService.page(page, queryWrapper);
        return ResultUtils.success(spacePage);
    }

    /**
     * 编辑空间（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest req,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(req == null || req.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 数据转换与填充
        Space space = new Space();
        BeanUtils.copyProperties(req, space);
        spaceService.fillSpaceBySpaceLevel(space);
        space.setEditTime(new Date());

        // 权限校验
        User loginUser = userService.getLoginUser(request);
        Space oldSpace = spaceService.getById(req.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(
                !oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR
        );

        // 执行更新
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取空间列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest req,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(req.getPageSize() > 20, ErrorCode.PARAMS_ERROR);
        Page<Space> page = new Page<>(req.getCurrent(), req.getPageSize());
        QueryWrapper<Space> queryWrapper = spaceService.getQueryWrapper(req);
        Page<Space> spacePage = spaceService.page(page, queryWrapper);
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }



}
