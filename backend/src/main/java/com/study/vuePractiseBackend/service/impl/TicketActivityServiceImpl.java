package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.TicketActivityDTO;
import com.study.vuePractiseBackend.dto.TicketActivityStatusDTO;
import com.study.vuePractiseBackend.dto.TicketActivityUpdateDTO;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import com.study.vuePractiseBackend.entity.TicketActivity;
import com.study.vuePractiseBackend.entity.TicketRecord;
import com.study.vuePractiseBackend.entity.TicketUser;
import com.study.vuePractiseBackend.mapper.TickerUserMapper;
import com.study.vuePractiseBackend.mapper.TicketActivityMapper;
import com.study.vuePractiseBackend.mapper.TicketRecordMapper;
import com.study.vuePractiseBackend.service.TicketActivityService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class TicketActivityServiceImpl extends ServiceImpl<TicketActivityMapper, TicketActivity> implements TicketActivityService {

    @Resource
    private TickerUserMapper tickerUserMapper;

    @Resource
    private TicketRecordMapper ticketRecordMapper;

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketActivity saveNewActivity(Long workspaceId, TicketActivityDTO dto) {
        lockWorkspace(workspaceId);
        if (workspaceId == null) {
            throw new IllegalArgumentException("工作空间不能为空");
        }
        if (dto == null || dto.getOperatorId() == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
        // 操作人必须是当前工作空间内正常启用的管理员。
        LambdaQueryWrapper<TicketUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(TicketUser::getWorkspaceId, workspaceId)
                .eq(TicketUser::getId, dto.getOperatorId())
                .eq(TicketUser::getRole, "ADMIN")
                .eq(TicketUser::getStatus, 1);
        TicketUser operator = tickerUserMapper.selectOne(userWrapper);
        if (operator == null) {
            return null;
        }
        String activityName = requireText(dto.getActivityName(), "活动名称", 100);
        String campus = requireText(dto.getCampus(), "校区", 50);
        if (!"新吴校区".equals(campus) && !"藕塘校区".equals(campus)) {
            throw new IllegalArgumentException("校区只能为新吴校区或藕塘校区");
        }
        String location = requireText(dto.getLocation(), "活动地点", 200);
        String coverUrl = dto.getCoverUrl();
        if (coverUrl != null) {
            coverUrl = coverUrl.trim();
            if (coverUrl.length() > 500) {
                throw new IllegalArgumentException("封面地址不能超过500个字符");
            }
            if (coverUrl.isEmpty()) {
                coverUrl = null;
            }
        }
        if (dto.getQuota() == null || dto.getQuota() <= 0) {
            throw new IllegalArgumentException("活动名额必须大于0");
        }
        LocalDateTime bookingStart = dto.getBookingStartTime();
        LocalDateTime bookingEnd = dto.getBookingEndTime();
        LocalDateTime activityStart = dto.getActivityStartTime();
        LocalDateTime activityEnd = dto.getActivityEndTime();
        if (bookingStart == null || bookingEnd == null || activityStart == null || activityEnd == null) {
            throw new IllegalArgumentException("报名和活动的开始、结束时间不能为空");
        }
        if (!bookingStart.isBefore(bookingEnd)) {
            throw new IllegalArgumentException("报名开始时间必须早于报名结束时间");
        }
        if (bookingEnd.isAfter(activityStart)) {
            throw new IllegalArgumentException("报名结束时间不能晚于活动开始时间");
        }
        if (!activityStart.isBefore(activityEnd)) {
            throw new IllegalArgumentException("活动开始时间必须早于活动结束时间");
        }
        TicketActivity activity = new TicketActivity();
        activity.setWorkspaceId(workspaceId);
        activity.setActivityName(activityName);
        activity.setDescription(dto.getDescription());
        activity.setCoverUrl(coverUrl);
        activity.setCampus(campus);
        activity.setLocation(location);
        // 状态、报名人数由后端设置。
        activity.setStatus(0);
        activity.setQuota(dto.getQuota());
        activity.setBookedCount(0);
        activity.setBookingStartTime(bookingStart);
        activity.setBookingEndTime(bookingEnd);
        activity.setActivityStartTime(activityStart);
        activity.setActivityEndTime(activityEnd);
        if (baseMapper.insert(activity) != 1) {
            throw new IllegalStateException("新建活动失败");
        }
        return activity;
    }

    /**
     * 必填文本：去除首尾空格，校验非空和长度。
     */
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

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketActivity updateActivity(Long workspaceId, Long activityId, TicketActivityUpdateDTO dto) {
        lockWorkspace(workspaceId);
        if (dto == null) {
            throw new IllegalArgumentException("活动信息不能为空");
        }
        checkAdmin(workspaceId, dto.getOperatorId());
        TicketActivity activity = findActivityForUpdate(workspaceId, activityId);
        Integer currentStatus = activity.getStatus();
        if (!Integer.valueOf(0).equals(currentStatus) && !Integer.valueOf(1).equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前活动状态不允许修改");
        }
        LocalDateTime now = LocalDateTime.now();
        // 已发布且开始报名后，四个时间字段必须保持一致。
        if (Integer.valueOf(1).equals(currentStatus) && activity.getBookingStartTime() != null && !now.isBefore(activity.getBookingStartTime())) {
            boolean timeChanged =
                    !Objects.equals(
                            activity.getBookingStartTime(),
                            dto.getBookingStartTime())
                            || !Objects.equals(
                            activity.getBookingEndTime(),
                            dto.getBookingEndTime())
                            || !Objects.equals(
                            activity.getActivityStartTime(),
                            dto.getActivityStartTime())
                            || !Objects.equals(
                            activity.getActivityEndTime(),
                            dto.getActivityEndTime());
            if (timeChanged) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "报名开始后不能修改报名及活动时间");
            }
        }
        // 校验并复制可修改字段，不修改状态、空间和报名人数。
        copyValidatedFields(activity, dto);
        if (activity.getQuota() < activity.getBookedCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "活动名额不能小于已有报名人数");
        }
        if (baseMapper.updateById(activity) != 1) {
            throw new IllegalStateException("修改活动失败");
        }
        return activity;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketActivity changeActivityStatus(Long workspaceId, Long activityId, TicketActivityStatusDTO dto) {
        lockWorkspace(workspaceId);
        if (dto == null) {
            throw new IllegalArgumentException("状态信息不能为空");
        }
        Integer targetStatus = dto.getStatus();
        if (targetStatus == null || (targetStatus != 1 && targetStatus != 2)) {
            throw new IllegalArgumentException("目标状态只能为1发布或2关闭");
        }
        checkAdmin(workspaceId, dto.getOperatorId());
        TicketActivity activity = findActivityForUpdate(workspaceId, activityId);
        Integer currentStatus = activity.getStatus();
        // 重复请求保持幂等。
        if (targetStatus.equals(currentStatus)) {
            return activity;
        }
        if (Integer.valueOf(0).equals(currentStatus) && targetStatus == 1) {
            // 发布前再次检查现有数据。
            checkPublishable(activity, LocalDateTime.now());
        } else if (Integer.valueOf(1).equals(currentStatus) && targetStatus == 2) {
            // 已发布活动可以关闭，保留报名记录。

        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅允许草稿发布，或已发布活动关闭");
        }
        activity.setStatus(targetStatus);
        if (baseMapper.updateById(activity) != 1) {
            throw new IllegalStateException("修改活动状态失败");
        }
        return activity;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer deleteActivity(Long workspaceId, Long activityId, Long operatorId) {
        lockWorkspace(workspaceId);
        checkAdmin(workspaceId, operatorId);
        TicketActivity activity = findActivityForUpdate(workspaceId, activityId);
        if (!Integer.valueOf(0).equals(activity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能删除草稿活动");
        }
        Long recordCount = ticketRecordMapper.selectCount(
                new LambdaQueryWrapper<TicketRecord>()
                        .eq(TicketRecord::getWorkspaceId, workspaceId)
                        .eq(TicketRecord::getActivityId, activityId));

        // 包括已取消的记录，不能删除已有历史记录的活动。
        if (recordCount > 0 || activity.getBookedCount() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "活动已有抢票记录或报名人数，不能删除");
        }
        int result = baseMapper.delete(
                new LambdaQueryWrapper<TicketActivity>()
                        .eq(TicketActivity::getWorkspaceId, workspaceId)
                        .eq(TicketActivity::getId, activityId));
        if (result != 1) {
            throw new IllegalStateException("删除活动失败");
        }
        return 1;
    }

    /**
     * 校验当前空间内的模拟管理员。
     */
    private void checkAdmin(Long workspaceId, Long operatorId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("工作空间不能为空");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("操作人ID必须为正整数");
        }
        TicketUser operator = tickerUserMapper.selectOne(
                new LambdaQueryWrapper<TicketUser>()
                        .eq(TicketUser::getWorkspaceId, workspaceId)
                        .eq(TicketUser::getId, operatorId)
                        .eq(TicketUser::getRole, "ADMIN")
                        .eq(TicketUser::getStatus, 1));
        if (operator == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "操作人不存在、已停用或不是当前空间的管理员");
        }
    }

    /**
     * 在当前空间内查询并锁定活动。
     * 必须在外层事务中调用。
     */
    private TicketActivity findActivityForUpdate(Long workspaceId, Long activityId) {
        if (activityId == null || activityId <= 0) {
            throw new IllegalArgumentException("活动ID必须为正整数");
        }
        TicketActivity activity = baseMapper.selectOne(
                new LambdaQueryWrapper<TicketActivity>()
                        .eq(TicketActivity::getWorkspaceId, workspaceId)
                        .eq(TicketActivity::getId, activityId)
                        .last("FOR UPDATE"));
        if (activity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "活动不存在");
        }
        return activity;
    }

    /**
     * 校验并复制新建、修改共用的字段。
     * requireText沿用之前已添加的方法。
     */
    private void copyValidatedFields(TicketActivity activity, TicketActivityDTO dto) {
        String activityName = requireText(dto.getActivityName(), "活动名称", 100);
        String campus = requireText(dto.getCampus(), "校区", 50);
        if (!"新吴校区".equals(campus) && !"藕塘校区".equals(campus)) {
            throw new IllegalArgumentException("校区只能为新吴校区或藕塘校区");
        }
        String location = requireText(dto.getLocation(), "活动地点", 200);
        String coverUrl = dto.getCoverUrl();
        if (coverUrl != null) {
            coverUrl = coverUrl.trim();
            if (coverUrl.length() > 500) {
                throw new IllegalArgumentException("封面地址不能超过500个字符");
            }
            if (coverUrl.isEmpty()) {
                coverUrl = null;
            }
        }
        if (dto.getQuota() == null || dto.getQuota() <= 0) {
            throw new IllegalArgumentException("活动名额必须大于0");
        }
        checkTimes(
                dto.getBookingStartTime(),
                dto.getBookingEndTime(),
                dto.getActivityStartTime(),
                dto.getActivityEndTime());
        activity.setActivityName(activityName);
        activity.setDescription(dto.getDescription());
        activity.setCoverUrl(coverUrl);
        activity.setCampus(campus);
        activity.setLocation(location);
        activity.setQuota(dto.getQuota());
        activity.setBookingStartTime(dto.getBookingStartTime());
        activity.setBookingEndTime(dto.getBookingEndTime());
        activity.setActivityStartTime(dto.getActivityStartTime());
        activity.setActivityEndTime(dto.getActivityEndTime());
    }

    private void checkTimes(LocalDateTime bookingStart, LocalDateTime bookingEnd, LocalDateTime activityStart, LocalDateTime activityEnd) {
        if (bookingStart == null || bookingEnd == null || activityStart == null || activityEnd == null) {
            throw new IllegalArgumentException("报名和活动的开始、结束时间不能为空");
        }
        if (!bookingStart.isBefore(bookingEnd)) {
            throw new IllegalArgumentException("报名开始时间必须早于报名结束时间");
        }
        if (bookingEnd.isAfter(activityStart)) {
            throw new IllegalArgumentException("报名结束时间不能晚于活动开始时间");
        }
        if (!activityStart.isBefore(activityEnd)) {
            throw new IllegalArgumentException("活动开始时间必须早于活动结束时间");
        }
    }

    /**
     * 发布时检查草稿的完整性及报名是否仍可进行。
     */
    private void checkPublishable(TicketActivity activity, LocalDateTime now) {
        requireText(activity.getActivityName(), "活动名称", 100);
        requireText(activity.getLocation(), "活动地点", 200);
        String campus = activity.getCampus();
        if (!"新吴校区".equals(campus) && !"藕塘校区".equals(campus)) {
            throw new IllegalArgumentException("校区只能为新吴校区或藕塘校区");
        }
        if (activity.getQuota() == null || activity.getQuota() <= 0) {
            throw new IllegalArgumentException("活动名额必须大于0");
        }
        if (activity.getBookedCount() == null || activity.getBookedCount() < 0 || activity.getQuota() < activity.getBookedCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "活动报名人数或名额异常");
        }
        checkTimes(
                activity.getBookingStartTime(),
                activity.getBookingEndTime(),
                activity.getActivityStartTime(),
                activity.getActivityEndTime());

        if (!now.isBefore(activity.getBookingEndTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "报名已经结束，不能发布活动");
        }
    }
    /** Serialize activity writes with workspace reset/deletion and ticket record writes. */
    private void lockWorkspace(Long workspaceId) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("工作空间ID必须为正数");
        }
        SysWorkspace workspace = sysWorkspaceMapper.selectOne(
                new LambdaQueryWrapper<SysWorkspace>()
                        .eq(SysWorkspace::getId, workspaceId).last("FOR UPDATE"));
        if (workspace == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工作空间不存在");
        }
        if (!Integer.valueOf(1).equals(workspace.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "工作空间已暂停");
        }
    }

}
