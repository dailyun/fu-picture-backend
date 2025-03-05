package com.dali.yupicturebackend.model.dto;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dali.yupicturebackend.mapper.SpaceMapper;
import com.dali.yupicturebackend.model.entity.Picture;
import com.dali.yupicturebackend.model.entity.Space;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpaceDAO  extends ServiceImpl<SpaceMapper, Space> {

    public boolean isExistSpaceByUserId(Long id) {
        return true;
    }



}

