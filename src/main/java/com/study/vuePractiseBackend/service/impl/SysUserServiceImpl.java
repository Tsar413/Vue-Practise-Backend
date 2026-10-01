package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.mapper.SysUserMapper;
import com.study.vuePractiseBackend.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

}
