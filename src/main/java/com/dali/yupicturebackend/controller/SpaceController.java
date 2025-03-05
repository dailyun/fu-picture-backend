package com.dali.yupicturebackend.controller;

import com.dali.yupicturebackend.annotation.AuthCheck;
import com.dali.yupicturebackend.common.BaseResponse;
import com.dali.yupicturebackend.common.ResultUtils;
import com.dali.yupicturebackend.constant.UserConstant;
import com.dali.yupicturebackend.exception.BusinessException;
import com.dali.yupicturebackend.exception.ErrorCode;
import com.dali.yupicturebackend.exception.ThrowUtils;
import com.dali.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.dali.yupicturebackend.model.dto.space.SpaceUpdateRequest;
import com.dali.yupicturebackend.model.entity.Space;
import com.dali.yupicturebackend.model.entity.User;
import com.dali.yupicturebackend.model.enums.SpaceLevelEnum;
import com.dali.yupicturebackend.model.vo.SpaceLevel;
import com.dali.yupicturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/space")
@RestController
public class SpaceController {

    @Resource
    private SpaceService spaceService;


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



}
