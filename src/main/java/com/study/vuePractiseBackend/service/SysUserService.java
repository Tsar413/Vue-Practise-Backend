package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.SysUserDTO;
import com.study.vuePractiseBackend.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    Integer createOneSysUser(SysUserDTO sysUserDTO);
}
