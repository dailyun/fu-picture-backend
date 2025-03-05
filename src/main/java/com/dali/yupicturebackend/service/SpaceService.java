package com.dali.yupicturebackend.service;

import com.dali.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.dali.yupicturebackend.model.entity.Picture;
import com.dali.yupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dali.yupicturebackend.model.entity.User;

/**
* @author 86159
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-03 15:45:01
*/
public interface SpaceService extends IService<Space> {

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);



}
