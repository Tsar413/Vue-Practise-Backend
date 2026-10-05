package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.entity.TicketActivity;
import com.study.vuePractiseBackend.entity.TicketRecord;
import com.study.vuePractiseBackend.entity.TicketUser;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import com.study.vuePractiseBackend.mapper.TickerUserMapper;
import com.study.vuePractiseBackend.mapper.TicketActivityMapper;
import com.study.vuePractiseBackend.mapper.TicketRecordMapper;
import com.study.vuePractiseBackend.service.TicketRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.notFound;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.forbidden;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.conflict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TicketRecordServiceImpl extends ServiceImpl<TicketRecordMapper, TicketRecord> implements TicketRecordService {

    @Resource
    private TickerUserMapper tickerUserMapper;

    @Resource
    private TicketActivityMapper ticketActivityMapper;

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    /**
     * 抢票。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketRecord bookTicket(Long workspaceId, Long activityId, Long userId) {
        checkId(activityId, "活动ID");
        checkId(userId, "用户ID");
        lockWorkspace(workspaceId);
        requireUser(workspaceId, userId, "USER");
        TicketActivity activity = requireActivity(workspaceId, activityId, true);
        LocalDateTime now = LocalDateTime.now();
        if (!Integer.valueOf(1).equals(activity.getStatus())) {
            throw conflict("活动未发布或已关闭，不能报名");
        }
        LocalDateTime start = activity.getBookingStartTime();
        LocalDateTime end = activity.getBookingEndTime();
        if (start == null || end == null || !start.isBefore(end)) {
            throw conflict("活动报名时间配置异常");
        }
        // 开始时间包含，结束时间不包含。
        if (now.isBefore(start)) {
            throw conflict("报名尚未开始");
        }
        if (!now.isBefore(end)) {
            throw conflict("报名已经结束");
        }
        // 有效和已核销票券均禁止重复报名。
        Long existingCount = baseMapper.selectCount(
                new LambdaQueryWrapper<TicketRecord>()
                        .eq(TicketRecord::getWorkspaceId, workspaceId)
                        .eq(TicketRecord::getActivityId, activityId)
                        .eq(TicketRecord::getUserId, userId)
                        .in(TicketRecord::getStatus, 1, 2));
        if (existingCount > 0) {
            throw conflict("该用户已报名，不能重复抢票");
        }
        checkActivityCounts(activity);
        if (activity.getBookedCount() >= activity.getQuota()) {
            throw conflict("活动名额已满");
        }
        TicketRecord record = new TicketRecord();
        record.setWorkspaceId(workspaceId);
        record.setActivityId(activityId);
        record.setUserId(userId);
        record.setTicketNo(generateTicketNo());
        record.setStatus(1);
        record.setBookingTime(now);
        record.setCancelTime(null);
        record.setVerifyTime(null);
        record.setVerifyUserId(null);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        if (baseMapper.insert(record) != 1) {
            throw new IllegalStateException("生成票券失败");
        }
        activity.setBookedCount(activity.getBookedCount() + 1);
        saveActivity(activity);
        return record;
    }

    /**
     * 取消票券。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketRecord cancelTicket(Long workspaceId, Long recordId, Long userId) {
        checkId(recordId, "记录ID");
        checkId(userId, "用户ID");
        lockWorkspace(workspaceId);
        requireUser(workspaceId, userId, "USER");
        // 先读取活动ID，再按活动、记录的顺序加锁。
        TicketRecord reference = requireRecord(workspaceId, recordId, false);
        if (!Objects.equals(reference.getUserId(), userId)) {
            throw forbidden("只能取消本人的票券");
        }
        TicketActivity activity = requireActivity(workspaceId, reference.getActivityId(), true);
        TicketRecord record = requireRecord(workspaceId, recordId, true);
        if (!Objects.equals(record.getUserId(), userId)) {
            throw forbidden("只能取消本人的票券");
        }
        // 重复取消返回原记录，不再次扣减人数。
        if (Integer.valueOf(0).equals(record.getStatus())) {
            return record;
        }
        if (!Integer.valueOf(1).equals(record.getStatus())) {
            throw conflict("仅有效票券可以取消");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getActivityStartTime() == null) {
            throw conflict("活动开始时间配置异常");
        }
        if (!now.isBefore(activity.getActivityStartTime())) {
            throw conflict("活动已开始，不能取消票券");
        }
        checkActivityCounts(activity);
        if (activity.getBookedCount() <= 0) {
            throw conflict("活动报名人数异常，无法释放名额");
        }
        record.setStatus(0);
        record.setCancelTime(now);
        record.setUpdateTime(now);
        saveRecord(record);
        activity.setBookedCount(activity.getBookedCount() - 1);
        saveActivity(activity);
        return record;
    }

    /**
     * 查询某个模拟用户的记录。
     */
    @Override
    public List<TicketRecord> getUserRecords(Long workspaceId, Long userId, Integer status, Long activityId) {
        requireUser(workspaceId, userId, "USER");
        checkRecordStatus(status);
        if (activityId != null) {
            requireActivity(workspaceId, activityId, false);
        }
        LambdaQueryWrapper<TicketRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketRecord::getWorkspaceId, workspaceId)
                .eq(TicketRecord::getUserId, userId);
        if (status != null) {
            wrapper.eq(TicketRecord::getStatus, status);
        }
        if (activityId != null) {
            wrapper.eq(TicketRecord::getActivityId, activityId);
        }
        wrapper.orderByDesc(TicketRecord::getId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 管理员查询活动报名名单。
     */
    @Override
    public List<TicketRecord> getActivityRecords(Long workspaceId, Long activityId, Long operatorId, Integer status) {
        requireUser(workspaceId, operatorId, "ADMIN");
        checkRecordStatus(status);
        requireActivity(workspaceId, activityId, false);
        LambdaQueryWrapper<TicketRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketRecord::getWorkspaceId, workspaceId)
                .eq(TicketRecord::getActivityId, activityId);
        if (status != null) {
            wrapper.eq(TicketRecord::getStatus, status);
        }
        wrapper.orderByDesc(TicketRecord::getId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 本人或管理员查询单张票券。
     */
    @Override
    public TicketRecord getRecordDetail(Long workspaceId, Long recordId, Long operatorId) {
        TicketUser operator = requireUser(workspaceId, operatorId, null);
        TicketRecord record = requireRecord(workspaceId, recordId, false);
        if (!"ADMIN".equals(operator.getRole()) && !Objects.equals(record.getUserId(), operatorId)) {
            throw forbidden("只能查询本人的票券");
        }
        return record;
    }

    /**
     * 管理员核销票券。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketRecord verifyTicket(Long workspaceId, Long recordId, Long operatorId) {
        checkId(recordId, "记录ID");
        checkId(operatorId, "操作人ID");
        lockWorkspace(workspaceId);
        requireUser(workspaceId, operatorId, "ADMIN");
        TicketRecord reference = requireRecord(workspaceId, recordId, false);
        // 加锁顺序：工作空间 → 活动 → 记录。
        requireActivity(workspaceId, reference.getActivityId(), true);
        TicketRecord record = requireRecord(workspaceId, recordId, true);
        if (Integer.valueOf(0).equals(record.getStatus())) {
            throw conflict("票券已取消，不能核销");
        }
        if (Integer.valueOf(2).equals(record.getStatus())) {
            throw conflict("票券已核销，不能重复核销");
        }
        if (!Integer.valueOf(1).equals(record.getStatus())) {
            throw conflict("票券状态异常，不能核销");
        }
        LocalDateTime now = LocalDateTime.now();
        record.setStatus(2);
        record.setVerifyTime(now);
        record.setVerifyUserId(operatorId);
        record.setUpdateTime(now);
        saveRecord(record);
        // 核销不释放名额，不改变bookedCount。
        return record;
    }

    /**
     * 与初始化、重置、空间删除共用工作空间行锁。
     * 只能在事务内调用。
     */
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

    /**
     * requiredRole为null时，允许正常启用的USER或ADMIN。
     */
    private TicketUser requireUser(Long workspaceId, Long userId, String requiredRole) {
        checkId(workspaceId, "工作空间ID");
        checkId(userId, "模拟用户ID");
        TicketUser user = tickerUserMapper.selectOne(
                new LambdaQueryWrapper<TicketUser>()
                        .eq(TicketUser::getWorkspaceId, workspaceId)
                        .eq(TicketUser::getId, userId));
        if (user == null) {
            throw notFound("模拟用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw forbidden("模拟用户已停用");
        }
        if (!"USER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw forbidden("模拟用户角色异常");
        }
        if (requiredRole != null && !requiredRole.equals(user.getRole())) {
            throw forbidden("ADMIN".equals(requiredRole) ? "仅管理员可以执行此操作" : "仅普通用户可以报名或取消票券");
        }
        return user;
    }

    private TicketActivity requireActivity(Long workspaceId, Long activityId, boolean forUpdate) {
        checkId(workspaceId, "工作空间ID");
        checkId(activityId, "活动ID");
        LambdaQueryWrapper<TicketActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketActivity::getWorkspaceId, workspaceId)
                .eq(TicketActivity::getId, activityId);
        if (forUpdate) {
            wrapper.last("FOR UPDATE");
        }
        TicketActivity activity = ticketActivityMapper.selectOne(wrapper);
        if (activity == null) {
            throw notFound("活动不存在");
        }
        return activity;
    }

    private TicketRecord requireRecord(Long workspaceId, Long recordId, boolean forUpdate) {
        checkId(workspaceId, "工作空间ID");
        checkId(recordId, "记录ID");
        LambdaQueryWrapper<TicketRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketRecord::getWorkspaceId, workspaceId)
                .eq(TicketRecord::getId, recordId);
        if (forUpdate) {
            wrapper.last("FOR UPDATE");
        }
        TicketRecord record = baseMapper.selectOne(wrapper);
        if (record == null) {
            throw notFound("票券记录不存在");
        }
        return record;
    }

    private void checkId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "必须为正整数");
        }
    }

    private void checkRecordStatus(Integer status) {
        if (status != null && status != 0 && status != 1 && status != 2) {
            throw new IllegalArgumentException("记录状态只能为0、1或2");
        }
    }

    private void checkActivityCounts(TicketActivity activity) {
        Integer quota = activity.getQuota();
        Integer bookedCount = activity.getBookedCount();
        if (quota == null || quota <= 0 || bookedCount == null || bookedCount < 0 || bookedCount > quota) {
            throw conflict("活动名额或报名人数异常");
        }
    }

    private void saveActivity(TicketActivity activity) {
        if (ticketActivityMapper.updateById(activity) != 1) {
            throw new IllegalStateException("更新活动报名人数失败");
        }
    }

    private void saveRecord(TicketRecord record) {
        if (baseMapper.updateById(record) != 1) {
            throw new IllegalStateException("更新票券记录失败");
        }
    }

    private String generateTicketNo() {
        return "TP" + UUID.randomUUID().toString().replace("-", "");
    }

}