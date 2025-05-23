package com.dali.fupicturebackend.model.dto;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dali.fupicturebackend.mapper.SpaceMapper;
import com.dali.fupicturebackend.model.entity.Space;
import org.springframework.stereotype.Service;

@Service
public class SpaceDAO  extends ServiceImpl<SpaceMapper, Space> {

    public boolean isExistSpaceByUserId(Long id) {
        return true;
    }



}

