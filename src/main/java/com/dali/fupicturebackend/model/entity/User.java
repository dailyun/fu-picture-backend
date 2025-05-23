package com.dali.fupicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


import java.util.Date;

/**
 * 用户
 * @TableName user
 */
@Data
@TableName(value ="user")
public class User {
    /**
     * id
     * -- GETTER --
     *  id

     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     * -- GETTER --
     *  账号

     */
    private String userAccount;

    /**
     * 密码
     * -- GETTER --
     *  密码

     */
    private String userPassword;

    /**
     * 用户昵称
     * -- GETTER --
     *  用户昵称

     */
    private String userName;

    /**
     * 用户头像
     * -- GETTER --
     *  用户头像

     */
    private String userAvatar;

    /**
     * 用户简介
     * -- GETTER --
     *  用户简介

     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     * -- GETTER --
     *  用户角色：user/admin

     */
    private String userRole;

    /**
     * 编辑时间
     * -- GETTER --
     *  编辑时间

     */
    private Date editTime;

    /**
     * 创建时间
     * -- GETTER --
     *  创建时间

     */
    private Date createTime;

    /**
     * 更新时间
     * -- GETTER --
     *  更新时间

     */
    private Date updateTime;

    /**
     * 是否删除
     * -- GETTER --
     *  是否删除

     */
    @TableLogic
    private Integer isDelete;

    /**
     * id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 账号
     */
    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    /**
     * 密码
     */
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * 用户昵称
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 用户头像
     */
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    /**
     * 用户简介
     */
    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }

    /**
     * 用户角色：user/admin
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * 编辑时间
     */
    public void setEditTime(Date editTime) {
        this.editTime = editTime;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 是否删除
     */
    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }
}