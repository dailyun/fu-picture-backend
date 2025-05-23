package com.dali.fupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dali.fupicturebackend.model.dto.space.SpaceAddRequest;
import com.dali.fupicturebackend.model.dto.space.SpaceDeleteRequest;
import com.dali.fupicturebackend.model.dto.space.SpaceQueryRequest;
import com.dali.fupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dali.fupicturebackend.model.entity.User;
import com.dali.fupicturebackend.model.vo.SpaceVO;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
* @author 86159
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-03 15:45:01
*/
@Service
public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     *
     * @param space space
     * @param add   是否为创建时检验
     */
    void validSpace(Space space, boolean add);

    boolean deleteSpace(SpaceDeleteRequest spaceDeleteRequest, User loginUser);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<SpaceVO> getSpaceVOPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Space> page, HttpServletRequest request);

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间对象
     */
    void fillSpaceBySpaceLevel(Space space);
}


