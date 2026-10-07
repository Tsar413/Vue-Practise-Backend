package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.StudentImportResultDTO;
import com.study.vuePractiseBackend.dto.SysUserDTO;
import com.study.vuePractiseBackend.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

public interface SysUserService extends IService<SysUser> {
    Integer createOneSysUser(SysUserDTO sysUserDTO);

    StudentImportResultDTO importStudents(MultipartFile file);

    Integer changeUser(SysUserDTO sysUserDTO);

    Integer changeUserStatus(String id, Integer status);

    Integer deleteById(String id);
}
