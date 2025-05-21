package com.dali.yupicturebackend.manager.auth;


import cn.hutool.core.io.resource.ResourceUtil;

import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.StrUtil;
import com.dali.yupicturebackend.manager.auth.model.SpaceUserAuthConfig;

import com.dali.yupicturebackend.manager.auth.model.SpaceUserRole;
import com.dali.yupicturebackend.model.entity.Space;
import com.dali.yupicturebackend.model.entity.SpaceUser;
import com.dali.yupicturebackend.model.entity.User;
import com.dali.yupicturebackend.model.enums.SpaceRoleEnum;
import com.dali.yupicturebackend.model.enums.SpaceTypeEnum;
import com.dali.yupicturebackend.service.SpaceUserService;
import com.dali.yupicturebackend.service.UserService;
import org.springframework.stereotype.Component;


import java.util.List;

import javax.annotation.Resource;

import java.util.ArrayList;


@Component
public class SpaceUserAuthManager {


    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }


    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }






}
