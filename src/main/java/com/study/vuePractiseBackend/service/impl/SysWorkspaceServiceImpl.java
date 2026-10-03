package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import com.study.vuePractiseBackend.service.SysWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SysWorkspaceServiceImpl extends ServiceImpl<SysWorkspaceMapper, SysWorkspace> implements SysWorkspaceService {
    /**
     * 不存在则创建，已存在则同步状态。
     * 恢复历史空间时保留原来的ID和项目数据。
     */
    @Override
    @Transactional
    public void ensureStudentWorkspace(String studentId, Integer status) {
        studentId = checkStudentId(studentId);
        checkStatus(status);
        SysWorkspace workspace = findByStudentId(studentId);
        if (workspace != null) {
            updateWorkspaceStatus(workspace, status);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        workspace = new SysWorkspace();
        workspace.setStudentId(studentId);
        workspace.setStatus(status);
        workspace.setCreateTime(now);
        workspace.setUpdateTime(now);
        if (baseMapper.insert(workspace) != 1) {
            throw new IllegalStateException("创建工作空间失败");
        }
        // TODO 复制两个项目的基准数据
    }

    /**
     * 同步学生工作空间状态，不存在则报错。
     */
    @Override
    @Transactional
    public void syncStudentWorkspaceStatus(String studentId, Integer status) {
        studentId = checkStudentId(studentId);
        checkStatus(status);
        SysWorkspace workspace = findByStudentId(studentId);
        if (workspace == null) {
            throw new IllegalStateException("学生工作空间不存在");
        }
        updateWorkspaceStatus(workspace, status);
    }

    /**
     * 学生转为教师时暂停历史空间，不存在则跳过。
     */
    @Override
    @Transactional
    public void pauseWorkspaceIfPresent(String studentId) {
        studentId = checkStudentId(studentId);
        SysWorkspace workspace = findByStudentId(studentId);
        if (workspace == null) {
            return;
        }
        updateWorkspaceStatus(workspace, 0);
    }

    /**
     * 删除用户时删除对应空间，不存在则跳过。
     */
    @Override
    @Transactional
    public void deleteWorkspaceIfPresent(String studentId) {
        studentId = checkStudentId(studentId);
        SysWorkspace workspace = findByStudentId(studentId);
        if (workspace == null) {
            return;
        }

        // TODO 根据workspace.getId()清理两个项目的关联数据

        if (baseMapper.deleteById(workspace.getId()) != 1) {
            throw new IllegalStateException("删除工作空间失败");
        }
    }

    /**
     * 根据学号查询唯一工作空间。
     */
    private SysWorkspace findByStudentId(String studentId) {
        LambdaQueryWrapper<SysWorkspace> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysWorkspace::getStudentId, studentId);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 更新状态，已经一致时无需重复写入。
     */
    private void updateWorkspaceStatus(SysWorkspace workspace, Integer status) {
        if (status.equals(workspace.getStatus())) {
            return;
        }
        // 只更新本次需要修改的字段
        SysWorkspace update = new SysWorkspace();
        update.setId(workspace.getId());
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        if (baseMapper.updateById(update) != 1) {
            throw new IllegalStateException("修改工作空间状态失败");
        }
    }

    private String checkStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("学号不能为空");
        }
        String id = studentId.trim();
        if (id.length() > 50) {
            throw new IllegalArgumentException("学号不能超过50个字符");
        }
        return id;
    }

    private void checkStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("工作空间状态只能为0或1");
        }
    }
}
