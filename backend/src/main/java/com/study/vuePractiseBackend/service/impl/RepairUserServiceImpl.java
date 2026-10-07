package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.RepairUserDTO;
import com.study.vuePractiseBackend.entity.*;
import com.study.vuePractiseBackend.mapper.*;
import com.study.vuePractiseBackend.service.RepairUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.study.vuePractiseBackend.exception.BusinessExceptions.forbidden;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.notFound;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.conflict;

@Service
public class RepairUserServiceImpl extends ServiceImpl<RepairUserMapper, RepairUser> implements RepairUserService {

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    @Resource
    private RepairOrderMapper repairOrderMapper;

    @Resource
    private RepairOrderImageMapper repairOrderImageMapper;

    @Resource
    private RepairProcessRecordMapper repairProcessRecordMapper;

    @Resource
    private RepairEvaluationMapper repairEvaluationMapper;

    @Resource
    private RepairAttachmentMapper repairAttachmentMapper;

    @Override
    public RepairUser switchUser(Long workspaceId, String userNo) {
        if (userNo == null || userNo.isBlank()) {
            throw new IllegalArgumentException("模拟用户编号不能为空");
        }
        userNo = userNo.trim();
        if (userNo.length() > 50) {
            throw new IllegalArgumentException("模拟用户编号不能超过50个字符");
        }
        LambdaQueryWrapper<RepairUser> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(RepairUser::getWorkspaceId, workspaceId)
                .eq(RepairUser::getUserNo, userNo);
        RepairUser user = baseMapper.selectOne(wrapper);
        if (user == null) {
            throw notFound("模拟用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw forbidden("模拟用户已停用，无法切换");
        }
        if (!"REPORTER".equals(user.getRole())
                && !"ADMIN".equals(user.getRole())
                && !"MAINTAINER".equals(user.getRole())) {
            throw forbidden("模拟用户角色异常");
        }
        return user;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairUser createUser(Long workspaceId, RepairUserDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        lockWorkspace(workspaceId);
        checkAdmin(workspaceId, dto.getOperatorId());

        String userNo = requireText(dto.getUserNo(), "用户编号", 50);
        String realName = requireText(dto.getRealName(), "姓名", 50);
        String role = checkRole(dto.getRole());
        String phone = optionalText(dto.getPhone(), "联系电话", 20);
        String department = optionalText(dto.getDepartment(), "部门", 100);

        Integer status = dto.getStatus() == null ? 1 : dto.getStatus();
        checkStatus(status);

        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<RepairUser>()
                        .eq(RepairUser::getWorkspaceId, workspaceId)
                        .eq(RepairUser::getUserNo, userNo));
        if (count > 0) {
            throw conflict("当前工作空间内用户编号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        RepairUser user = new RepairUser();
        user.setWorkspaceId(workspaceId);
        user.setUserNo(userNo);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setDepartment(department);
        user.setRole(role);
        user.setStatus(status);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        if (baseMapper.insert(user) != 1) {
            throw new IllegalStateException("创建模拟用户失败");
        }
        return user;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairUser updateUser(Long workspaceId, Long userId, RepairUserDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        checkId(userId, "用户ID");
        lockWorkspace(workspaceId);
        checkAdmin(workspaceId, dto.getOperatorId());

        RepairUser user = findUser(workspaceId, userId);

        String userNo = requireText(dto.getUserNo(), "用户编号", 50);
        if (!user.getUserNo().equals(userNo)) {
            throw new IllegalArgumentException("用户编号不允许修改");
        }

        String realName = requireText(dto.getRealName(), "姓名", 50);
        String role = checkRole(dto.getRole());
        String phone = optionalText(dto.getPhone(), "联系电话", 20);
        String department = optionalText(dto.getDepartment(), "部门", 100);

        Integer status = dto.getStatus() == null ? user.getStatus() : dto.getStatus();
        checkStatus(status);

        // 已参与业务的用户保留原角色，避免影响历史和当前工单。
        if (!role.equals(user.getRole()) && hasBusinessReferences(workspaceId, userId)) {
            throw conflict("用户已参与维修业务，不能修改角色");
        }

        LocalDateTime now = LocalDateTime.now();
        // 显式set，允许清空联系电话和部门。
        LambdaUpdateWrapper<RepairUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairUser::getWorkspaceId, workspaceId)
                .eq(RepairUser::getId, userId)
                .set(RepairUser::getRealName, realName)
                .set(RepairUser::getPhone, phone)
                .set(RepairUser::getDepartment, department)
                .set(RepairUser::getRole, role)
                .set(RepairUser::getStatus, status)
                .set(RepairUser::getUpdateTime, now);

        if (baseMapper.update(null, wrapper) != 1) {
            throw new IllegalStateException("修改模拟用户失败");
        }
        user.setRealName(realName);
        user.setPhone(phone);
        user.setDepartment(department);
        user.setRole(role);
        user.setStatus(status);
        user.setUpdateTime(now);
        return user;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer deleteUser(Long workspaceId, Long userId, Long operatorId) {

        checkId(userId, "用户ID");
        lockWorkspace(workspaceId);
        checkAdmin(workspaceId, operatorId);
        findUser(workspaceId, userId);

        if (hasBusinessReferences(workspaceId, userId)) {
            throw conflict("用户已有关联业务数据，请停用而不是删除");
        }
        int result = baseMapper.delete(
                new LambdaQueryWrapper<RepairUser>()
                        .eq(RepairUser::getWorkspaceId, workspaceId)
                        .eq(RepairUser::getId, userId));
        if (result != 1) {
            throw new IllegalStateException("删除模拟用户失败");
        }
        return 1;
    }

    private void lockWorkspace(Long workspaceId) {
        checkId(workspaceId, "工作空间ID");

        SysWorkspace workspace = sysWorkspaceMapper.selectOne(
                new LambdaQueryWrapper<SysWorkspace>()
                        .eq(SysWorkspace::getId, workspaceId)
                        .last("FOR UPDATE"));
        if (workspace == null) {
            throw notFound("工作空间不存在");
        }
        if (!Integer.valueOf(1).equals(workspace.getStatus())) {
            throw forbidden("工作空间已暂停");
        }
    }

    private void checkAdmin(Long workspaceId, Long operatorId) {
        checkId(operatorId, "操作人ID");
        RepairUser operator = findUser(workspaceId, operatorId);
        if (!Integer.valueOf(1).equals(operator.getStatus())) {
            throw forbidden("操作人已停用");
        }
        if (!"ADMIN".equals(operator.getRole())) {
            throw forbidden("仅管理员可以管理模拟用户");
        }
    }

    private RepairUser findUser(Long workspaceId, Long userId) {
        checkId(userId, "用户ID");
        RepairUser user = baseMapper.selectOne(
                new LambdaQueryWrapper<RepairUser>()
                        .eq(RepairUser::getWorkspaceId, workspaceId)
                        .eq(RepairUser::getId, userId));
        if (user == null) {
            throw notFound("模拟用户不存在");
        }
        return user;
    }

    /**
     * 检查报修人、维修人员、图片上传人、处理人及评价人关联。
     */
    private boolean hasBusinessReferences(Long workspaceId, Long userId) {
        return repairOrderMapper.selectCount(
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(RepairOrder::getWorkspaceId, workspaceId)
                        .and(w -> w
                                .eq(RepairOrder::getReporterId, userId)
                                .or()
                                .eq(RepairOrder::getMaintainerId, userId))) > 0

                || repairOrderImageMapper.selectCount(
                new LambdaQueryWrapper<RepairOrderImage>()
                        .eq(RepairOrderImage::getWorkspaceId, workspaceId)
                        .eq(RepairOrderImage::getUploaderId, userId)) > 0

                || repairProcessRecordMapper.selectCount(
                new LambdaQueryWrapper<RepairProcessRecord>()
                        .eq(RepairProcessRecord::getWorkspaceId, workspaceId)
                        .and(w -> w
                                .eq(RepairProcessRecord::getOperatorId, userId)
                                .or()
                                .eq(RepairProcessRecord::getTargetUserId, userId))) > 0

                || repairEvaluationMapper.selectCount(
                new LambdaQueryWrapper<RepairEvaluation>()
                        .eq(RepairEvaluation::getWorkspaceId, workspaceId)
                        .eq(RepairEvaluation::getUserId, userId)) > 0

                || repairAttachmentMapper.selectCount(
                new LambdaQueryWrapper<RepairAttachment>()
                        .eq(RepairAttachment::getWorkspaceId, workspaceId)
                        .eq(RepairAttachment::getUploaderId, userId)) > 0;
    }

    private String checkRole(String role) {role = requireText(role, "角色", 20);
        if (!"REPORTER".equals(role) && !"MAINTAINER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("角色只能为REPORTER、MAINTAINER或ADMIN");
        }
        return role;
    }

    private void checkStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态只能为0或1");
        }
    }

    private void checkId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "必须为正整数");
        }
    }

    private String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        value = value.trim();
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "不能超过" + maxLength + "个字符");
        }
        return value;
    }

    private String optionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "不能超过" + maxLength + "个字符");
        }
        return value.isEmpty() ? null : value;
    }
}
