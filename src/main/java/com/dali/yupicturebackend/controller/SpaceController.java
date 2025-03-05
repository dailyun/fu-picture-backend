package com.dali.yupicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dali.yupicturebackend.annotation.AuthCheck;
import com.dali.yupicturebackend.common.BaseResponse;
import com.dali.yupicturebackend.common.ResultUtils;
import com.dali.yupicturebackend.constant.UserConstant;
import com.dali.yupicturebackend.exception.BusinessException;
import com.dali.yupicturebackend.exception.ErrorCode;
import com.dali.yupicturebackend.exception.ThrowUtils;
import com.dali.yupicturebackend.model.dto.space.*;
import com.dali.yupicturebackend.model.entity.Space;
import com.dali.yupicturebackend.model.entity.User;
import com.dali.yupicturebackend.model.enums.SpaceLevelEnum;
import com.dali.yupicturebackend.model.vo.SpaceLevel;
import com.dali.yupicturebackend.model.vo.SpaceVO;
import com.dali.yupicturebackend.service.SpaceService;
import com.dali.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    public BaseResponse<Boolean> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, User loginUser) {

        spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(true);
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
        return ResultUtils.success(spaceService.getSpaceVO(space, request));
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
