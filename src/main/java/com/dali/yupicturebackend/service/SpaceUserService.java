package com.dali.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dali.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.dali.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.dali.yupicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dali.yupicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 86159
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-05-17 19:57:20
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser (SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
