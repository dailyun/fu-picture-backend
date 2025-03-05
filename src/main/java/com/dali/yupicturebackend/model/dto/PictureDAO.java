package com.dali.yupicturebackend.model.dto;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dali.yupicturebackend.mapper.PictureMapper;
import com.dali.yupicturebackend.model.entity.Picture;

import java.util.List;

public class PictureDAO  extends ServiceImpl<PictureMapper, Picture> {

    public void deleteBatchIds(List<Long> list) {
        this.removeByIds(list);
    }

}
