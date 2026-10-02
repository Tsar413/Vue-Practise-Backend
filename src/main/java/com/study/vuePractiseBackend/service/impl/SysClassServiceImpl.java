package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.SysClassDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.mapper.SysUserMapper;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import com.study.vuePractiseBackend.service.SysClassService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements SysClassService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    @Override
    @Transactional
    public Integer addNewClass(SysClassDTO sysClassDTO) {
        if (sysClassDTO == null || sysClassDTO.getId() == null || sysClassDTO.getClassName() == null) {
            return -2;
        }
        String id = sysClassDTO.getId().trim();
        String name = sysClassDTO.getClassName().trim();
        if (id.isBlank() || name.isBlank()) {
            return -2;
        }
        if(id.length() > 50 || name.length() > 100){
            return -3;
        }
        SysClass sysClass = baseMapper.selectById(id);
        if(sysClass != null){
            return -4;
        }
        sysClass = new SysClass();
        sysClass.setId(id);
        sysClass.setClassName(name);
        sysClass.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        sysClass.setCreateTime(now);
        sysClass.setUpdateTime(now);
        return baseMapper.insert(sysClass);
    }

    @Override
    @Transactional
    public Integer changeClass(SysClassDTO sysClassDTO) {
        if (sysClassDTO == null || sysClassDTO.getId() == null || sysClassDTO.getClassName() == null) {
            return -2;
        }
        String id = sysClassDTO.getId().trim();
        String name = sysClassDTO.getClassName().trim();
        if (id.isBlank() || name.isBlank()) {
            return -2;
        }
        if(id.length() > 50 || name.length() > 100){
            return -3;
        }
        SysClass sysClass = baseMapper.selectById(id);
        if(sysClass == null){
            return -4;
        }
        sysClass.setClassName(name);
        sysClass.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(sysClass);
    }

    @Override
    @Transactional
    public Integer deleteById(String id) {
        int result = baseMapper.deleteById(id);
        if (result != 1) {
            throw new IllegalStateException("删除用户状态失败");
        }
        QueryWrapper<SysUser> wrapper2 = new QueryWrapper<SysUser>();
        wrapper2.eq("class_id", id);
        List<SysUser> sysUsers = sysUserMapper.selectList(wrapper2);
        for (SysUser sysUser : sysUsers) {
            UpdateWrapper<SysWorkspace> wrapper3 = new UpdateWrapper<SysWorkspace>();
            wrapper3.eq("student_id", sysUser.getId());
            int result2 = sysWorkspaceMapper.delete(wrapper3);
            if (result2 != 1) {
                throw new IllegalStateException("删除工作空间状态失败");
            }
        }
        QueryWrapper<SysUser> wrapper1 = new QueryWrapper<SysUser>();
        wrapper1.eq("class_id", id);
        int result1 = sysUserMapper.delete(wrapper1);
        if (result1 < 0) {
            throw new IllegalStateException("删除学生失败");
        }
        // TODO 各项目workspace删除
        // TODO LoginToken待做
        return 1;
    }

    @Override
    @Transactional
    public Integer changeClassStatus(String id, int status) {
        if (id == null) {
            return -2;
        }
        id = id.trim();
        if (id.isBlank()) {
            return -2;
        }
        if(id.length() > 50){
            return -3;
        }
        if (status != 0 && status != 1) {
            return -5;
        }
        SysClass sysClass = baseMapper.selectById(id);
        if(sysClass == null){
            return -4;
        }
        sysClass.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        sysClass.setUpdateTime(now);
        int result = baseMapper.updateById(sysClass);
        if (result != 1) {
            throw new IllegalStateException("修改班级状态失败");
        }
        UpdateWrapper<SysUser> wrapper1 = new UpdateWrapper<SysUser>();
        wrapper1.set("status", status);
        wrapper1.eq("class_id", id);
        wrapper1.set("update_time", now);
        int result1 = sysUserMapper.update(wrapper1);
        if (result1 < 0) {
            throw new IllegalStateException("修改学生状态失败");
        }
        QueryWrapper<SysUser> wrapper2 = new QueryWrapper<SysUser>();
        wrapper2.eq("class_id", id);
        List<SysUser> sysUsers = sysUserMapper.selectList(wrapper2);
        for (SysUser sysUser : sysUsers) {
            UpdateWrapper<SysWorkspace> wrapper3 = new UpdateWrapper<SysWorkspace>();
            wrapper3.set("status", status);
            wrapper3.eq("student_id", sysUser.getId());
            wrapper3.set("update_time", now);
            int result2 = sysWorkspaceMapper.update(wrapper3);
            if (result2 != 1) {
                throw new IllegalStateException("修改工作空间状态失败");
            }
        }
        return 1;
    }
}
