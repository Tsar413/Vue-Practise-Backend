package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.SysUserDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.mapper.SysUserMapper;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import com.study.vuePractiseBackend.service.SysUserService;
import com.study.vuePractiseBackend.util.PasswordUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Override
    @Transactional
    public Integer createOneSysUser(SysUserDTO sysUserDTO) {
        if(sysUserDTO == null || sysUserDTO.getId() == null || sysUserDTO.getRealName() == null || sysUserDTO.getRole() == null){
            return -2;
        }
        String id = sysUserDTO.getId().trim();
        String username = sysUserDTO.getUsername() == null ? "" : sysUserDTO.getUsername().trim();
        String realName = sysUserDTO.getRealName().trim();
        String role = sysUserDTO.getRole().trim();
        String classId = sysUserDTO.getClassId() == null ? "" : sysUserDTO.getClassId().trim();
        if(id.isBlank() || realName.isBlank() || role.isBlank() || (!role.equals("TEACHER") && !role.equals("STUDENT"))){
            return -3;
        }
        if(id.length() > 50 || username.length() > 50 || realName.length() > 50 || classId.length() > 50){
            return -7;
        }
        if(role.equals("STUDENT")){
            if(!classId.isBlank()){
                SysClass sysClass = sysClassMapper.selectById(classId);
                if(sysClass == null){
                    return -5;
                }
                if(sysClass.getStatus() == 0){
                    return -8;
                }
            } else {
                return -4;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        SysUser sysUser = baseMapper.selectById(id);
        if(sysUser != null){
            return -6;
        }
        sysUser = new SysUser();
        sysUser.setId(id);
        sysUser.setUsername(username);
        if(username.isBlank()){
            sysUser.setUsername(realName);
        }
        sysUser.setRealName(realName);
        sysUser.setRole(role);
        sysUser.setClassId("STUDENT".equals(role) ? classId : null);
        sysUser.setStatus(1);
        sysUser.setCreateTime(now);
        sysUser.setUpdateTime(now);
        sysUser.setLastLoginTime(null);
        String salt = PasswordUtil.generateSalt();

        sysUser.setPasswordSalt(salt);
        sysUser.setPasswordHash(PasswordUtil.hashPassword("123456", salt));
        sysUser.setPasswordAlgorithm("SHA-256");

        int userResult = baseMapper.insert(sysUser);
        if (userResult != 1) {
            throw new IllegalStateException("创建用户失败");
        }

        if ("STUDENT".equals(role)) {
            SysWorkspace sysWorkspace = new SysWorkspace();
            sysWorkspace.setStudentId(id);
            sysWorkspace.setStatus(1);
            sysWorkspace.setCreateTime(now);
            sysWorkspace.setUpdateTime(now);

            int workspaceResult = sysWorkspaceMapper.insert(sysWorkspace);
            if (workspaceResult != 1) {
                throw new IllegalStateException("创建工作空间失败");
            }
        }

        return 1;
    }
}
