package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.SysClassDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.service.SysClassService;
import com.study.vuePractiseBackend.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements SysClassService {

    @Resource
    private SysUserService sysUserService;

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
        // 用户与工作空间由用户Service统一清理；任一失败使整个班级操作回滚。
        // 先删除关联用户，再删除班级。
        List<SysUser> sysUsers = findClassUsers(id);
        for (SysUser sysUser : sysUsers) {
            Integer result = sysUserService.deleteById(sysUser.getId());
            if (!Integer.valueOf(1).equals(result)) {
                throw new IllegalStateException("删除班级关联用户失败");
            }
        }

        if (baseMapper.deleteById(id) != 1) {
            throw new IllegalStateException("删除班级失败");
        }

        // LoginToken与项目数据清理由用户、工作空间Service中的TODO统一处理
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
        // 即使班级状态未变化，也同步关联用户及其工作空间。
        for (SysUser sysUser : findClassUsers(id)) {
            Integer userResult = sysUserService.changeUserStatus(sysUser.getId(), status);
            if (!Integer.valueOf(1).equals(userResult)) {
                throw new IllegalStateException("修改班级关联用户状态失败");
            }
        }
        return 1;
    }

    private List<SysUser> findClassUsers(String classId) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", classId);
        return sysUserService.list(wrapper);
    }
}
