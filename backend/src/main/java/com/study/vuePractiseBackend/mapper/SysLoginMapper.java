package com.study.vuePractiseBackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.vuePractiseBackend.entity.SysLoginToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLoginMapper extends BaseMapper<SysLoginToken> {
}
