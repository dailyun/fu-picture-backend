package com.dali.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dali.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.dali.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.dali.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dali.yupicturebackend.model.entity.User;
import com.dali.yupicturebackend.model.picture.PictureQueryRequest;
import com.dali.yupicturebackend.model.picture.PictureReviewRequest;
import com.dali.yupicturebackend.model.vo.PictureVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 86159
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-02-25 19:19:45
*/
@Service
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     *
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */


    // 上传图片
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);
    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );




    void deletePictureService(long pictureId, User loginUser);

    void checkPictureAuth(User loginUser, Picture picture);

    List<Picture> getListBySpaceId(Long spaceId, Long userId);

    void clearPictureFile(Picture picture);
}
