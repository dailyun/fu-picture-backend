package com.dali.fupicturebackend.utils;

import com.dali.fupicturebackend.exception.BusinessException;
import com.dali.fupicturebackend.exception.ErrorCode;
import com.dali.fupicturebackend.model.entity.Picture;
import com.dali.fupicturebackend.model.entity.User;
import com.dali.fupicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class pictureUtils {

    @Resource
    private UserService userService;
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }
}
