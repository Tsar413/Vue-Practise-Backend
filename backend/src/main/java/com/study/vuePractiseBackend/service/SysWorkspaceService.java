package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.entity.SysWorkspace;

import java.util.List;

public interface SysWorkspaceService extends IService<SysWorkspace> {
    // 学生创建时分配；已有历史空间时恢复，并同步目标状态
    void ensureStudentWorkspace(String studentId, Integer status);

    // 用户启停时同步，学生空间不存在则报错
    void syncStudentWorkspaceStatus(String studentId, Integer status);

    // 学生转为教师时暂停历史空间，不存在则无需处理
    void pauseWorkspaceIfPresent(String studentId);

    // 删除用户时清理工作空间，不存在允许跳过
    void deleteWorkspaceIfPresent(String studentId);

    List<SysWorkspace> getWorkspacesClassId(String id);

    Integer changeWorkspaceStatus(String studentId, Integer status);

    Integer initializeTicketClass(String classId);

    Integer initializeRepairClass(String classId);

    Integer resetWorkspace(String studentId, String project);
}
