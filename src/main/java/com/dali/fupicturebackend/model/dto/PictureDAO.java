package com.dali.fupicturebackend.model.dto;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dali.fupicturebackend.mapper.PictureMapper;
import com.dali.fupicturebackend.model.entity.Picture;

import java.util.List;

public class PictureDAO  extends ServiceImpl<PictureMapper, Picture> {

    public void deleteBatchIds(List<Long> list) {
        this.removeByIds(list);
    }

}
