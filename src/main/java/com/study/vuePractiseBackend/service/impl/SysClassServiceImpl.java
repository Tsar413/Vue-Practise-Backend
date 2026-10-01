package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.SysClassDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.service.SysClassService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements SysClassService {
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
        // TODO 检查班级是否存在学生
        int result = baseMapper.deleteById(id);

        return result;
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
        if (status == sysClass.getStatus()) {
            return 1;
        }
        sysClass.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        sysClass.setUpdateTime(now);
        int result = baseMapper.updateById(sysClass);
        // TODO SysUser的status状态修改0/1切换 与班级一致
        return result;
    }
}
