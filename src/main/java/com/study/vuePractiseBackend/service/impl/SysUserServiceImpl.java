package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.StudentImportResultDTO;
import com.study.vuePractiseBackend.dto.SysUserDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.mapper.SysClassMapper;
import com.study.vuePractiseBackend.mapper.SysUserMapper;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import com.study.vuePractiseBackend.service.SysUserService;
import com.study.vuePractiseBackend.util.PasswordUtil;
import com.study.vuePractiseBackend.util.StudentExcelUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

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

    @Override
    public StudentImportResultDTO importStudents(MultipartFile file) {
        List<StudentExcelUtil.StudentRow> rows = StudentExcelUtil.readStudents(file);
        StudentImportResultDTO report = new StudentImportResultDTO();
        report.setTotal(rows.size());

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (StudentExcelUtil.StudentRow row : rows) {

            // 文件中的单行格式错误
            if (row.errorMessage() != null) {
                report.setFailedCount(report.getFailedCount() + 1);
                report.getDetails().add(
                        new StudentImportResultDTO.RowResult(
                                row.rowNumber(),
                                row.id(),
                                "FAILED",
                                row.errorMessage()
                        )
                );
                continue;
            }

            SysUserDTO dto = new SysUserDTO();
            dto.setId(row.id());
            dto.setRealName(row.realName());
            dto.setUsername(row.realName());
            dto.setClassId(row.classId());
            dto.setRole("STUDENT");

            try {
                Integer result = template.execute(transactionStatus -> {
                    // 优先检查编号：已存在则跳过，不修改已有账号
                    if (baseMapper.selectById(row.id()) != null) {
                        return -6;
                    }
                    Integer createResult = createOneSysUser(dto);
                    if (createResult == null || createResult != 1) {
                        transactionStatus.setRollbackOnly();
                    }
                    return createResult;
                });

                // execute返回时，该行事务已经处理完成
                if (Integer.valueOf(1).equals(result)) {
                    report.setSuccessCount(report.getSuccessCount() + 1);
                } else if (Integer.valueOf(-6).equals(result)) {
                    report.setSkippedCount(report.getSkippedCount() + 1);

                    report.getDetails().add(
                            new StudentImportResultDTO.RowResult(
                                    row.rowNumber(),
                                    row.id(),
                                    "SKIPPED",
                                    "用户编号已存在，未修改已有账号"
                            )
                    );
                } else {
                    report.setFailedCount(report.getFailedCount() + 1);

                    report.getDetails().add(
                            new StudentImportResultDTO.RowResult(
                                    row.rowNumber(),
                                    row.id(),
                                    "FAILED",
                                    getImportFailureMessage(result)
                            )
                    );
                }
            } catch (RuntimeException e) {
                // 在单行事务外捕获：该行已回滚，后续行继续处理
                log.error(
                        "学生导入失败，Excel行号：{}，学号：{}",
                        row.rowNumber(), row.id(), e
                );
                report.setFailedCount(report.getFailedCount() + 1);
                report.getDetails().add(
                        new StudentImportResultDTO.RowResult(
                                row.rowNumber(),
                                row.id(),
                                "FAILED",
                                "创建失败，该行事务未成功完成，请检查后端日志"
                        )
                );
            }
        }
        return report;
    }

    private String getImportFailureMessage(Integer result) {
        if (result == null) {
            return "创建失败";
        }
        return switch (result) {
            case -2 -> "必填字段缺失";
            case -3 -> "用户信息不合法";
            case -4 -> "班级编号不能为空";
            case -5 -> "班级不存在";
            case -7 -> "字段长度超过限制";
            case -8 -> "班级已停用";
            default -> "创建失败";
        };
    }

    @Override
    @Transactional
    public Integer changeUser(SysUserDTO sysUserDTO) {
        if (sysUserDTO == null || sysUserDTO.getId() == null || sysUserDTO.getRealName() == null || sysUserDTO.getRole() == null) {
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
        if ("STUDENT".equals(role)) {
            if (classId.isBlank()) {
                return -5;
            }
            SysClass sysClass = sysClassMapper.selectById(classId);
            if (sysClass == null) {
                return -5;
            }
            if (!Integer.valueOf(1).equals(sysClass.getStatus())) {
                return -8;
            }
        }
        SysUser sysUser = baseMapper.selectById(id);
        if(sysUser == null){
            return -4;
        }
        LocalDateTime now = LocalDateTime.now();
        String changeRole = sysUser.getRole();
        UpdateWrapper<SysUser> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id);
        wrapper.set("role", role);
        wrapper.set("username", username.isBlank() ? realName : username);
        wrapper.set("real_name", realName);
        wrapper.set("class_id", "TEACHER".equals(role) ? null : classId);
        wrapper.set("update_time", now);
        int result = baseMapper.update(null, wrapper);
        if (result != 1) {
            throw new IllegalStateException("修改用户失败");
        }
        if(!role.equals(changeRole)){
            QueryWrapper<SysWorkspace> wrapper1 = new QueryWrapper<SysWorkspace>();
            wrapper1.eq("student_id", id);
            List<SysWorkspace> sysWorkspaces = sysWorkspaceMapper.selectList(wrapper1);
            if("STUDENT".equals(role)){
                if(sysWorkspaces.isEmpty()){
                    SysWorkspace sysWorkspace = new SysWorkspace();
                    sysWorkspace.setStudentId(id);
                    sysWorkspace.setStatus(sysUser.getStatus());
                    sysWorkspace.setCreateTime(now);
                    sysWorkspace.setUpdateTime(now);

                    int workspaceResult = sysWorkspaceMapper.insert(sysWorkspace);
                    if (workspaceResult != 1) {
                        throw new IllegalStateException("创建工作空间失败");
                    }
                } else {
                    SysWorkspace sysWorkspace = sysWorkspaces.getFirst();
                    sysWorkspace.setStatus(sysUser.getStatus());
                    sysWorkspace.setUpdateTime(now);
                    int workspaceResult = sysWorkspaceMapper.updateById(sysWorkspace);
                    if (workspaceResult != 1) {
                        throw new IllegalStateException("修改工作空间失败");
                    }
                }
            } else {
                if (!sysWorkspaces.isEmpty()) {
                    SysWorkspace sysWorkspace = sysWorkspaces.getFirst();
                    sysWorkspace.setStatus(0);
                    sysWorkspace.setUpdateTime(now);
                    int workspaceResult = sysWorkspaceMapper.updateById(sysWorkspace);
                    if (workspaceResult != 1) {
                        throw new IllegalStateException("修改工作空间失败");
                    }
                }
            }
        }
        return 1;
    }

    @Override
    @Transactional
    public Integer changeUserStatus(String id, Integer status) {
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
        if (status == null || (status != 0 && status != 1)) {
            return -5;
        }
        SysUser sysUser = baseMapper.selectById(id);
        if(sysUser == null){
            return -4;
        }
        sysUser.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        sysUser.setUpdateTime(now);
        int result = baseMapper.updateById(sysUser);
        if (result != 1) {
            throw new IllegalStateException("修改用户状态失败");
        }
        if("STUDENT".equals(sysUser.getRole())){
            QueryWrapper<SysWorkspace> wrapper1 = new QueryWrapper<SysWorkspace>();
            wrapper1.eq("student_id", id);
            List<SysWorkspace> sysWorkspaces = sysWorkspaceMapper.selectList(wrapper1);
            if (sysWorkspaces.isEmpty()) {
                throw new IllegalStateException("学生工作空间不存在");
            }
            SysWorkspace sysWorkspace = sysWorkspaces.getFirst();
            sysWorkspace.setStatus(status);
            sysWorkspace.setUpdateTime(now);
            int workspaceResult = sysWorkspaceMapper.updateById(sysWorkspace);
            if (workspaceResult != 1) {
                throw new IllegalStateException("修改工作空间失败");
            }
        }
        // TODO LoginToken待做
        return 1;
    }

    @Override
    @Transactional
    public Integer deleteById(String id) {
        int result = baseMapper.deleteById(id);
        if (result != 1) {
            throw new IllegalStateException("删除用户状态失败");
        }
        QueryWrapper<SysWorkspace> wrapper1 = new QueryWrapper<SysWorkspace>();
        wrapper1.eq("student_id", id);
        int result1 = sysWorkspaceMapper.delete(wrapper1);
        if(result1 < 0){
            throw new IllegalStateException("删除用户空间失败");
        }
        // TODO LoginToken待做
        // TODO 各项目workspace删除
        return 1;
    }
}
