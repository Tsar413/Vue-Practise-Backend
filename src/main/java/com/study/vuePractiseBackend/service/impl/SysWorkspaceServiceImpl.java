package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.entity.*;
import com.study.vuePractiseBackend.mapper.*;
import com.study.vuePractiseBackend.service.SysWorkspaceService;
import com.study.vuePractiseBackend.util.RepairMockDataUtil;
import com.study.vuePractiseBackend.util.TicketMockDataUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SysWorkspaceServiceImpl extends ServiceImpl<SysWorkspaceMapper, SysWorkspace> implements SysWorkspaceService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private TickerUserMapper tickerUserMapper;

    @Resource
    private TicketActivityMapper ticketActivityMapper;

    @Resource
    private TicketRecordMapper ticketRecordMapper;

    @Resource
    private RepairUserMapper repairUserMapper;

    @Resource
    private RepairDeviceMapper repairDeviceMapper;

    @Resource
    private RepairEvaluationMapper repairEvaluationMapper;

    @Resource
    private RepairOrderMapper repairOrderMapper;

    @Resource
    private RepairOrderImageMapper repairOrderImageMapper;

    @Resource
    private RepairProcessRecordMapper repairProcessRecordMapper;

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
     * 删除用户时清理两个项目的数据，再删除工作空间。
     * 空间不存在则跳过。
     */
    @Override
    @Transactional
    public void deleteWorkspaceIfPresent(String studentId) {
        studentId = checkStudentId(studentId);
        SysWorkspace workspace = baseMapper.selectByStudentIdForUpdate(studentId);
        if (workspace == null) {
            return;
        }
        Long workspaceId = workspace.getId();
        deleteTicketData(workspaceId);
        deleteRepairData(workspaceId);
        if (baseMapper.deleteById(workspaceId) != 1) {
            throw new IllegalStateException("删除工作空间失败");
        }
    }

    @Override
    public List<SysWorkspace> getWorkspacesClassId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("班级编号不能为空");
        }
        // 第一次查询：只取学生学号
        QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
        userWrapper.select("id");
        userWrapper.eq("class_id", id.trim());
        userWrapper.eq("role", "STUDENT");

        List<SysUser> users = sysUserMapper.selectList(userWrapper);
        // 必须处理空集合，避免空IN条件带来的问题
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> studentIds = users.stream()
                .map(SysUser::getId)
                .toList();

        // 第二次查询：一次性获取这些学生的工作空间
        QueryWrapper<SysWorkspace> workspaceWrapper = new QueryWrapper<>();
        workspaceWrapper.in("student_id", studentIds);
        workspaceWrapper.orderByAsc("student_id");
        return baseMapper.selectList(workspaceWrapper);
    }

    @Override
    @Transactional
    public Integer changeWorkspaceStatus(String studentId, Integer status) {
        if (studentId == null || studentId.isBlank()) {
            return -2;
        }
        studentId = studentId.trim();
        if (studentId.length() > 50) {
            return -3;
        }
        if (status == null || (status != 0 && status != 1)) {
            return -5;
        }
        SysWorkspace workspace = findByStudentId(studentId);
        if (workspace == null) {
            return -4;
        }
        // 启用前检查资格，即使空间已经启用也要检查
        if (status == 1) {
            SysUser user = sysUserMapper.selectById(studentId);
            if (user == null) {
                return -6;
            }
            if (!"STUDENT".equals(user.getRole())) {
                return -7;
            }
            if (!Integer.valueOf(1).equals(user.getStatus())) {
                return -8;
            }
            if (user.getClassId() == null || user.getClassId().isBlank()) {
                return -10;
            }
            SysClass sysClass = sysClassMapper.selectById(user.getClassId());
            if (sysClass == null) {
                return -10;
            }
            if (!Integer.valueOf(1).equals(sysClass.getStatus())) {
                return -11;
            }
        }
        updateWorkspaceStatus(workspace, status);
        return 1;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer initializeTicketClass(String classId) {
        if (classId == null || classId.isBlank()) {
            return -2;
        }
        classId = classId.trim();
        if (classId.length() > 50) {
            return -3;
        }
        SysClass sysClass = sysClassMapper.selectById(classId);
        if(sysClass == null){
            return -4;
        }
        QueryWrapper<SysUser> wrapper1 = new QueryWrapper<SysUser>();
        wrapper1.eq("class_id", classId)
                .eq("role", "STUDENT")
                .orderByAsc("id");
        List<SysUser> sysUsers = sysUserMapper.selectList(wrapper1);
        if(sysUsers.isEmpty()){
            return -5;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SysUser sysUser : sysUsers) {
            SysWorkspace workspace = baseMapper.selectByStudentIdForUpdate(sysUser.getId());
            if (workspace == null) {
                throw new IllegalStateException("学生工作空间不存在：" + sysUser.getId());
            }
            Long workspaceId = workspace.getId();
            if (hasTicketData(workspaceId)) {
                continue;
            }
            createTicketData(workspaceId, sysUser, now);
        }
        return 1;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer initializeRepairClass(String classId) {
        if (classId == null || classId.isBlank()) {
            return -2;
        }
        classId = classId.trim();
        if (classId.length() > 50) {
            return -3;
        }
        SysClass sysClass = sysClassMapper.selectById(classId);
        if (sysClass == null) {
            return -4;
        }
        QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
        userWrapper.eq("class_id", classId)
                   .eq("role", "STUDENT")
                   .orderByAsc("id");
        List<SysUser> sysUsers = sysUserMapper.selectList(userWrapper);
        if (sysUsers.isEmpty()) {
            return -5;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SysUser sysUser : sysUsers) {
            SysWorkspace workspace = baseMapper.selectByStudentIdForUpdate(sysUser.getId());
            if (workspace == null) {
                throw new IllegalStateException("学生工作空间不存在：" + sysUser.getId());
            }
            Long workspaceId = workspace.getId();
            if (hasRepairData(workspaceId)) {
                continue;
            }
            createRepairData(workspaceId, sysUser, now);
        }
        return 1;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer resetWorkspace(String studentId, String project) {
        if (studentId == null || studentId.isBlank()) {
            return -2;
        }
        studentId = studentId.trim();
        if (studentId.length() > 50) {
            return -3;
        }
        if (project == null || project.isBlank()) {
            return -5;
        }
        project = project.trim().toUpperCase(Locale.ROOT);
        if (!"TICKET".equals(project) && !"REPAIR".equals(project) && !"ALL".equals(project)) {
            return -5;
        }
        // 与初始化、删除使用同一把工作空间行锁。
        SysWorkspace workspace = baseMapper.selectByStudentIdForUpdate(studentId);
        if (workspace == null) {
            return -4;
        }
        SysUser sysUser = sysUserMapper.selectById(studentId);
        if (sysUser == null) {
            return -6;
        }
        if (!"STUDENT".equals(sysUser.getRole())) {
            return -7;
        }
        Long workspaceId = workspace.getId();
        LocalDateTime now = LocalDateTime.now();
        if ("TICKET".equals(project) || "ALL".equals(project)) {
            deleteTicketData(workspaceId);
            createTicketData(workspaceId, sysUser, now);
        }
        if ("REPAIR".equals(project) || "ALL".equals(project)) {
            deleteRepairData(workspaceId);
            createRepairData(workspaceId, sysUser, now);
        }
        return 1;
    }

    private boolean hasTicketData(Long workspaceId) {
        return tickerUserMapper.selectCount(
                new QueryWrapper<TicketUser>()
                        .eq("workspace_id", workspaceId)) > 0
                || ticketActivityMapper.selectCount(
                new QueryWrapper<TicketActivity>()
                        .eq("workspace_id", workspaceId)) > 0
                || ticketRecordMapper.selectCount(
                new QueryWrapper<TicketRecord>()
                        .eq("workspace_id", workspaceId)) > 0;
    }

    /**
     * 任意Repair表中已有该工作空间的数据，就跳过初始化。
     */
    private boolean hasRepairData(Long workspaceId) {
        return repairUserMapper.selectCount(
                new QueryWrapper<RepairUser>()
                        .eq("workspace_id", workspaceId)) > 0

                || repairDeviceMapper.selectCount(
                new QueryWrapper<RepairDevice>()
                        .eq("workspace_id", workspaceId)) > 0

                || repairOrderMapper.selectCount(
                new QueryWrapper<RepairOrder>()
                        .eq("workspace_id", workspaceId)) > 0

                || repairProcessRecordMapper.selectCount(
                new QueryWrapper<RepairProcessRecord>()
                        .eq("workspace_id", workspaceId)) > 0

                || repairOrderImageMapper.selectCount(
                new QueryWrapper<RepairOrderImage>()
                        .eq("workspace_id", workspaceId)) > 0

                || repairEvaluationMapper.selectCount(
                new QueryWrapper<RepairEvaluation>()
                        .eq("workspace_id", workspaceId)) > 0;
    }

    private void createTicketData(Long workspaceId, SysUser sysUser, LocalDateTime time){
        List<TicketUser> ticketUsers = TicketMockDataUtil.createUsers(workspaceId);
        List<TicketActivity> ticketActivities = TicketMockDataUtil.createActivities(workspaceId, time);
        for (TicketUser ticketUser : ticketUsers) {
            if (tickerUserMapper.insert(ticketUser) != 1) {
                throw new IllegalStateException("初始化抢票用户失败：" + sysUser.getId());
            }
        }
        for (TicketActivity ticketActivity : ticketActivities) {
            if (ticketActivityMapper.insert(ticketActivity) != 1) {
                throw new IllegalStateException("初始化抢票活动失败：" + sysUser.getId());
            }
        }
    }

    private void createRepairData(Long workspaceId, SysUser sysUser, LocalDateTime time){
        // 1. 生成并保存模拟用户，回填主键。
        List<RepairUser> users = RepairMockDataUtil.createUsers(workspaceId, time);
        for (RepairUser user : users) {
            if (repairUserMapper.insert(user) != 1) {
                throw new IllegalStateException("初始化维修用户失败：" + sysUser.getId());
            }
        }
        // 2. 生成并保存设备，回填主键。
        List<RepairDevice> devices = RepairMockDataUtil.createDevices(workspaceId, time);
        for (RepairDevice device : devices) {
            if (repairDeviceMapper.insert(device) != 1) {
                throw new IllegalStateException(
                        "初始化维修设备失败：" + sysUser.getId());
            }
        }
        // 3. 使用真实用户、设备ID生成工单。
        // 保留Mock数据中已有的BX编号。
        List<RepairOrder> orders = RepairMockDataUtil.createOrders(workspaceId, users, devices, time);
        for (RepairOrder order : orders) {
            if (repairOrderMapper.insert(order) != 1) {
                throw new IllegalStateException("初始化维修工单失败：" + sysUser.getId());
            }
        }
        // 4. 使用真实工单ID生成处理记录。
        List<RepairProcessRecord> records = RepairMockDataUtil.createProcessRecords(orders);
        for (RepairProcessRecord record : records) {
            if (repairProcessRecordMapper.insert(record) != 1) {
                throw new IllegalStateException("初始化维修处理记录失败：" + sysUser.getId());
            }
        }
    }

    /**
     * 清理Ticket数据，保留工作空间。
     */
    private void deleteTicketData(Long workspaceId) {
        ticketRecordMapper.delete(
                new QueryWrapper<TicketRecord>()
                        .eq("workspace_id", workspaceId));
        ticketActivityMapper.delete(
                new QueryWrapper<TicketActivity>()
                        .eq("workspace_id", workspaceId));
        tickerUserMapper.delete(
                new QueryWrapper<TicketUser>()
                        .eq("workspace_id", workspaceId));
    }

    /**
     * 清理Repair数据，保留工作空间。
     */
    private void deleteRepairData(Long workspaceId) {
        repairOrderImageMapper.delete(
                new QueryWrapper<RepairOrderImage>()
                        .eq("workspace_id", workspaceId));
        repairEvaluationMapper.delete(
                new QueryWrapper<RepairEvaluation>()
                        .eq("workspace_id", workspaceId));
        repairProcessRecordMapper.delete(
                new QueryWrapper<RepairProcessRecord>()
                        .eq("workspace_id", workspaceId));
        repairOrderMapper.delete(
                new QueryWrapper<RepairOrder>()
                        .eq("workspace_id", workspaceId));
        repairDeviceMapper.delete(
                new QueryWrapper<RepairDevice>()
                        .eq("workspace_id", workspaceId));
        repairUserMapper.delete(
                new QueryWrapper<RepairUser>()
                        .eq("workspace_id", workspaceId));
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
