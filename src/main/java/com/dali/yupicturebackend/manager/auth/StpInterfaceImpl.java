package com.dali.yupicturebackend.manager.auth;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.dali.yupicturebackend.exception.BusinessException;
import com.dali.yupicturebackend.exception.ErrorCode;
import com.dali.yupicturebackend.manager.auth.model.SpaceUserAuthContext;
import com.dali.yupicturebackend.manager.auth.model.SpaceUserPermission;
import com.dali.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.dali.yupicturebackend.model.entity.Picture;
import com.dali.yupicturebackend.model.entity.Space;
import com.dali.yupicturebackend.model.entity.SpaceUser;
import com.dali.yupicturebackend.model.entity.User;
import com.dali.yupicturebackend.model.enums.SpaceRoleEnum;
import com.dali.yupicturebackend.model.enums.SpaceTypeEnum;
import com.dali.yupicturebackend.service.PictureService;
import com.dali.yupicturebackend.service.SpaceService;
import com.dali.yupicturebackend.service.SpaceUserService;
import com.dali.yupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.dali.yupicturebackend.constant.UserConstant.USER_LOGIN_STATE;

public class StpInterfaceImpl {
    @Value("${server.servlet.context-path}")
    private String contextPath;

    private SpaceUserAuthManager spaceUserAuthManager;
    private SpaceUserService spaceUserService;
    private PictureService pictureService;
    private UserService userService;
    private SpaceService spaceService;

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post 操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }

    public List<String> getPermissionList(Object loginId, String loginType) {
        //  判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        //  获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        //  获取userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser ==null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }
        Long userId =loginUser.getId();

        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser =spaceUserService.getById(spaceUserId);
            if (spaceUser ==null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户不存在");
            }

            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw  new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
            spaceId = picture.getSpaceId();
            // 公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId)|| userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }

        }
        // 获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        //
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            if (space.getUserId().equals(userId)|| userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else{
                return new ArrayList<>();
            }
        } else {
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

    }
    private boolean isAllFieldsNull(Object obj) {
        if (obj == null) {
            return true;
        }
        return Arrays.stream(ReflectUtil.getFields(obj.getClass()))
                .map(field -> ReflectUtil.getFieldValue(obj, field))
                .allMatch(ObjectUtil::isEmpty);
    }
}
