package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.RepairAssignDTO;
import com.study.vuePractiseBackend.dto.RepairProcessDTO;
import com.study.vuePractiseBackend.dto.RepairSubmitDTO;
import com.study.vuePractiseBackend.dto.RepairOrderCreateDTO;
import com.study.vuePractiseBackend.dto.RepairReturnDTO;
import com.study.vuePractiseBackend.dto.RepairEvaluationDTO;
import com.study.vuePractiseBackend.entity.*;
import com.study.vuePractiseBackend.mapper.*;
import com.study.vuePractiseBackend.service.RepairOrderService;
import com.study.vuePractiseBackend.util.RepairContentUtil;
import com.study.vuePractiseBackend.vo.RepairOrderDetailVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.study.vuePractiseBackend.exception.BusinessExceptions.*;

@Service
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairOrderService {

    @Resource
    private RepairUserMapper repairUserMapper;

    @Resource
    private RepairDeviceMapper repairDeviceMapper;

    @Resource
    private RepairProcessRecordMapper repairProcessRecordMapper;

    @Resource
    private RepairAttachmentMapper repairAttachmentMapper;

    @Resource
    private RepairOrderImageMapper repairOrderImageMapper;

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    @Resource
    private RepairEvaluationMapper repairEvaluationMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairOrder confirmOrder(Long workspaceId, Long orderId, Long operatorId) {
        lockWorkspace(workspaceId);
        RepairUser operator = requireUser(workspaceId, operatorId);
        RepairOrder order = requireOrder(workspaceId, orderId, true);
        checkOriginalReporter(order, operator);
        if (Integer.valueOf(5).equals(order.getStatus())) return order;
        if (!Integer.valueOf(4).equals(order.getStatus())) {
            throw conflict("仅待确认工单可以确认完成");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(5);
        order.setCompletedTime(now);
        order.setUpdateTime(now);
        saveOrderOrThrow(order);
        createProcessRecord(order, operatorId, "CONFIRM", 4, 5, "报修人确认维修完成", now);
        return order;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairOrder returnOrder(Long workspaceId, Long orderId, RepairReturnDTO dto) {
        if (dto == null) throw new IllegalArgumentException("退回参数不能为空");
        lockWorkspace(workspaceId);
        RepairUser operator = requireUser(workspaceId, dto.getOperatorId());
        RepairOrder order = requireOrder(workspaceId, orderId, true);
        checkOriginalReporter(order, operator);
        if (!Integer.valueOf(4).equals(order.getStatus())) {
            throw conflict("仅待确认工单可以退回维修");
        }
        String content = requireText(dto.getContent(), "退回原因", 2000);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(3);
        order.setUpdateTime(now);
        // 保留原师傅、上一轮维修说明、图片及历史；重新提交必须使用新附件。
        saveOrderOrThrow(order);
        createProcessRecord(order, operator.getId(), "RETURN", 4, 3, content, now);
        return order;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairEvaluation evaluateOrder(Long workspaceId, Long orderId, RepairEvaluationDTO dto) {
        if (dto == null) throw new IllegalArgumentException("评价参数不能为空");
        lockWorkspace(workspaceId);
        RepairUser operator = requireUser(workspaceId, dto.getOperatorId());
        RepairOrder order = requireOrder(workspaceId, orderId, true);
        checkOriginalReporter(order, operator);
        if (!Integer.valueOf(5).equals(order.getStatus())) {
            throw conflict("仅已完成工单可以评价");
        }
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 5) {
            throw new IllegalArgumentException("评分只能为1至5");
        }
        String content = optionalText(dto.getContent(), "评价内容", 1000);
        if (repairEvaluationMapper.selectCount(new LambdaQueryWrapper<RepairEvaluation>()
                .eq(RepairEvaluation::getWorkspaceId, workspaceId)
                .eq(RepairEvaluation::getOrderId, orderId)) > 0) {
            throw conflict("该工单已经评价，不能重复评价");
        }
        LocalDateTime now = LocalDateTime.now();
        RepairEvaluation evaluation = new RepairEvaluation();
        evaluation.setWorkspaceId(workspaceId);
        evaluation.setOrderId(orderId);
        evaluation.setUserId(operator.getId());
        evaluation.setScore(dto.getScore());
        evaluation.setContent(content);
        evaluation.setCreateTime(now);
        evaluation.setUpdateTime(now);
        if (repairEvaluationMapper.insert(evaluation) != 1) {
            throw new IllegalStateException("保存工单评价失败");
        }
        return evaluation;
    }

    @Override
    public RepairEvaluation getEvaluation(Long workspaceId, Long orderId, Long operatorId) {
        RepairUser operator = requireUser(workspaceId, operatorId);
        RepairOrder order = requireOrder(workspaceId, orderId, false);
        checkViewPermission(workspaceId, order, operator);
        RepairEvaluation evaluation = repairEvaluationMapper.selectOne(
                new LambdaQueryWrapper<RepairEvaluation>()
                        .eq(RepairEvaluation::getWorkspaceId, workspaceId)
                        .eq(RepairEvaluation::getOrderId, orderId));
        if (evaluation == null) throw notFound("该工单尚未评价");
        return evaluation;
    }

    @Override
    public List<RepairProcessRecord> getProcessRecords(Long workspaceId, Long orderId, Long operatorId) {
        RepairUser operator = requireUser(workspaceId, operatorId);
        RepairOrder order = requireOrder(workspaceId, orderId, false);
        checkViewPermission(workspaceId, order, operator);
        // 校验工单查看权限后返回完整时间线，不只返回本人操作。
        return repairProcessRecordMapper.selectList(new LambdaQueryWrapper<RepairProcessRecord>()
                .eq(RepairProcessRecord::getWorkspaceId, workspaceId)
                .eq(RepairProcessRecord::getOrderId, orderId)
                .orderByAsc(RepairProcessRecord::getId));
    }

    private void checkOriginalReporter(RepairOrder order, RepairUser operator) {
        if (!"REPORTER".equals(operator.getRole())
                || !Objects.equals(order.getReporterId(), operator.getId())) {
            throw forbidden("仅原报修人可以操作");
        }
    }

    @Override
    public List<RepairOrder> getOrders(Long workspaceId, Long operatorId, Integer status, String campus, String deviceType, String keyword, String scope) {
        RepairUser operator = requireUser(workspaceId, operatorId);
        if (status != null && (status < 0 || status > 5)) {
            throw new IllegalArgumentException("工单状态只能为0至5");
        }
        campus = optionalText(campus, "校区", 50);
        deviceType = optionalText(deviceType, "设备类型", 50);
        keyword = optionalText(keyword, "关键词", 100);
        scope = optionalText(scope, "查询范围", 20);
        if (campus != null) {
            checkCampus(campus);
        }
        if (scope != null) {
            scope = scope.toUpperCase(Locale.ROOT);
            if (!"ASSIGNED".equals(scope) && !"PARTICIPATED".equals(scope)) {
                throw new IllegalArgumentException("scope只能为ASSIGNED或PARTICIPATED");
            }
        }
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairOrder::getWorkspaceId, workspaceId);
        if ("REPORTER".equals(operator.getRole())) {
            wrapper.eq(RepairOrder::getReporterId, operatorId);
        } else if ("MAINTAINER".equals(operator.getRole())) {
            if ("PARTICIPATED".equals(scope)) {
                List<Long> orderIds = findParticipatedOrderIds(workspaceId, operatorId);
                if (orderIds.isEmpty()) {
                    return Collections.emptyList();
                }
                wrapper.in(RepairOrder::getId, orderIds);
            } else {
                wrapper.eq(RepairOrder::getMaintainerId, operatorId);
            }
        }
        // ADMIN不追加用户归属条件，只限定当前空间。
        if (status != null) {
            wrapper.eq(RepairOrder::getStatus, status);
        }
        if (campus != null) {
            wrapper.eq(RepairOrder::getCampus, campus);
        }
        if (deviceType != null) {
            wrapper.eq(RepairOrder::getDeviceType, deviceType);
        }
        if (keyword != null) {
            String searchKeyword = keyword;
            wrapper.and(w -> w
                    .like(RepairOrder::getOrderNo, searchKeyword)
                    .or()
                    .like(RepairOrder::getTitle, searchKeyword));
        }
        wrapper.orderByDesc(RepairOrder::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public RepairOrderDetailVO getOrderDetail(Long workspaceId, Long orderId, Long operatorId) {
        RepairUser operator = requireUser(workspaceId, operatorId);
        RepairOrder order = requireOrder(workspaceId, orderId, false);
        checkViewPermission(workspaceId, order, operator);
        RepairOrderDetailVO result = new RepairOrderDetailVO();
        result.setOrder(order);
        result.setReporterName(findUserName(workspaceId, order.getReporterId()));
        result.setMaintainerName(findUserName(workspaceId, order.getMaintainerId()));
        return result;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairOrder createOrder(Long workspaceId, RepairOrderCreateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("报修信息不能为空");
        }
        lockWorkspace(workspaceId);
        RepairUser reporter = requireUser(workspaceId, dto.getOperatorId());
        if (!"REPORTER".equals(reporter.getRole())) {
            throw forbidden("仅报修人可以创建工单");
        }
        String title = requireText(dto.getTitle(), "工单标题", 100);
        String description = RepairContentUtil.cleanRequiredHtml(dto.getDescription(), "故障描述");
        String phone = optionalText(dto.getContactPhone(), "联系电话", 20);
        if (phone == null) {
            phone = optionalText(reporter.getPhone(), "联系电话", 20);
        }
        if (phone == null) {
            throw new IllegalArgumentException("联系电话不能为空");
        }
        RepairOrder order = new RepairOrder();
        if (dto.getDeviceId() != null) {
            checkId(dto.getDeviceId(), "设备ID");

            RepairDevice device = repairDeviceMapper.selectOne(
                    new LambdaQueryWrapper<RepairDevice>()
                            .eq(RepairDevice::getWorkspaceId, workspaceId)
                            .eq(RepairDevice::getId, dto.getDeviceId()));
            if (device == null) {
                throw notFound("设备不存在");
            }
            if (!Integer.valueOf(1).equals(device.getStatus())) {
                throw conflict("设备已停用，不能创建报修工单");
            }
            order.setDeviceId(device.getId());
            order.setDeviceName(device.getDeviceName());
            order.setDeviceType(device.getDeviceType());
            order.setCampus(device.getCampus());
            order.setLocation(device.getLocation());
        } else {
            String campus = requireText(dto.getCampus(), "校区", 50);
            checkCampus(campus);
            order.setDeviceId(null);
            order.setDeviceName(requireText(dto.getDeviceName(), "设备名称", 100));
            order.setDeviceType(requireText(dto.getDeviceType(), "设备类型", 50));
            order.setCampus(campus);
            order.setLocation(requireText(dto.getLocation(), "报修地点", 200));
        }

        // 图片必须是本人上传、当前空间内未关联的故障图片。
        List<RepairAttachment> attachments = requireFaultAttachments(workspaceId, reporter.getId(), dto.getImageIds());

        LocalDateTime now = LocalDateTime.now();

        order.setWorkspaceId(workspaceId);
        order.setOrderNo(generateOrderNo());
        order.setTitle(title);
        order.setDescription(description);
        order.setReporterId(reporter.getId());
        order.setContactPhone(phone);
        order.setMaintainerId(null);
        order.setStatus(1);
        order.setRepairResult(null);
        order.setCompletedTime(null);
        order.setCancelTime(null);
        order.setCreateTime(now);
        order.setUpdateTime(now);

        if (baseMapper.insert(order) != 1) {
            throw new IllegalStateException("创建工单失败");
        }

        RepairProcessRecord process = createProcessRecord(
                order, reporter.getId(), "CREATE",
                null, 1, "报修人提交工单", now);

        bindFaultAttachments(order, process, attachments, now);
        return order;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairOrder cancelOrder(Long workspaceId, Long orderId, Long operatorId) {
        checkId(orderId, "工单ID");
        lockWorkspace(workspaceId);
        RepairUser operator = requireUser(workspaceId, operatorId);
        if (!"REPORTER".equals(operator.getRole())) {
            throw forbidden("仅原报修人可以撤回工单");
        }
        RepairOrder order = requireOrder(workspaceId, orderId, true);
        if (!Objects.equals(order.getReporterId(), operatorId)) {
            throw forbidden("只能撤回本人提交的工单");
        }
        // 重复撤回直接返回，不重复追加处理记录。
        if (Integer.valueOf(0).equals(order.getStatus())) {
            return order;
        }
        if (!Integer.valueOf(1).equals(order.getStatus()) && !Integer.valueOf(2).equals(order.getStatus())) {
            throw conflict("只能撤回待分派或待接单的工单");
        }
        Integer fromStatus = order.getStatus();
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(0);
        order.setCancelTime(now);
        order.setUpdateTime(now);
        if (baseMapper.updateById(order) != 1) {
            throw new IllegalStateException("撤回工单失败");
        }
        createProcessRecord(
                order, operatorId, "CANCEL",
                fromStatus, 0, "报修人撤回工单", now);
        // 保留原维修人员、图片及历史记录。
        return order;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairOrder assignOrder(Long workspaceId, Long orderId, RepairAssignDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("派单参数不能为空");
        }
        lockWorkspace(workspaceId);
        RepairUser operator = requireUser(workspaceId, dto.getOperatorId());
        if (!"ADMIN".equals(operator.getRole())) {
            throw forbidden("仅管理员可以派单");
        }
        RepairOrder order = requireOrder(workspaceId, orderId, true);
        Integer fromStatus = order.getStatus();
        if (!Integer.valueOf(1).equals(fromStatus) && !Integer.valueOf(2).equals(fromStatus)) {
            throw conflict("仅待分派或待接单工单可以派单");
        }
        RepairUser maintainer = requireUser(workspaceId, dto.getMaintainerId());
        if (!"MAINTAINER".equals(maintainer.getRole())) {
            throw forbidden("目标用户必须是维修师傅");
        }
        String content = optionalText(dto.getContent(), "派单说明", 2000);
        // 重复分派给同一师傅，不更新工单、不追加记录。
        if (Integer.valueOf(2).equals(fromStatus)
                && Objects.equals(order.getMaintainerId(), maintainer.getId())) {
            return order;
        }
        LocalDateTime now = LocalDateTime.now();
        order.setMaintainerId(maintainer.getId());
        order.setStatus(2);
        order.setUpdateTime(now);
        saveOrderOrThrow(order);

        createProcessRecord(
                order,
                operator.getId(),
                "ASSIGN",
                fromStatus,
                2,
                maintainer.getId(),
                content == null ? "管理员分派工单" : content,
                now);

        return order;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairOrder acceptOrder(Long workspaceId, Long orderId, Long operatorId) {

        lockWorkspace(workspaceId);

        RepairUser operator = requireUser(workspaceId, operatorId);
        if (!"MAINTAINER".equals(operator.getRole())) {
            throw forbidden("仅维修师傅可以接单");
        }

        RepairOrder order = requireOrder(workspaceId, orderId, true);
        checkAssignedMaintainer(order, operator.getId());

        // 重复接单，不追加记录。
        if (Integer.valueOf(3).equals(order.getStatus())) {
            return order;
        }

        if (!Integer.valueOf(2).equals(order.getStatus())) {
            throw conflict("仅待接单工单可以接单");
        }

        LocalDateTime now = LocalDateTime.now();

        order.setStatus(3);
        order.setUpdateTime(now);

        saveOrderOrThrow(order);

        createProcessRecord(
                order,
                operator.getId(),
                "ACCEPT",
                2,
                3,
                "维修师傅接单",
                now);

        return order;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairProcessRecord addProcessRecord(
            Long workspaceId,
            Long orderId,
            RepairProcessDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("维修记录参数不能为空");
        }

        lockWorkspace(workspaceId);

        RepairUser operator = requireUser(workspaceId, dto.getOperatorId());
        if (!"MAINTAINER".equals(operator.getRole())) {
            throw forbidden("仅维修师傅可以追加维修记录");
        }

        RepairOrder order = requireOrder(workspaceId, orderId, true);
        checkAssignedMaintainer(order, operator.getId());

        if (!Integer.valueOf(3).equals(order.getStatus())) {
            throw conflict("仅维修中的工单可以追加维修记录");
        }

        String content = requireText(dto.getContent(), "维修过程说明", 2000);

        // 图片选传，必须为当前师傅上传的未绑定维修图片。
        List<RepairAttachment> attachments = requireAttachments(
                workspaceId,
                operator.getId(),
                dto.getImageIds(),
                2,
                false);

        LocalDateTime now = LocalDateTime.now();

        RepairProcessRecord process = createProcessRecord(
                order,
                operator.getId(),
                "RECORD",
                3,
                3,
                content,
                now);

        bindAttachments(order, process, attachments, 2, now);

        order.setUpdateTime(now);
        saveOrderOrThrow(order);

        return process;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairOrder submitOrder(
            Long workspaceId,
            Long orderId,
            RepairSubmitDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("维修结果参数不能为空");
        }

        lockWorkspace(workspaceId);

        RepairUser operator = requireUser(workspaceId, dto.getOperatorId());
        if (!"MAINTAINER".equals(operator.getRole())) {
            throw forbidden("仅维修师傅可以提交维修结果");
        }

        RepairOrder order = requireOrder(workspaceId, orderId, true);
        checkAssignedMaintainer(order, operator.getId());

        // 已提交成功：返回原工单，不再校验已绑定图片或追加记录。
        if (Integer.valueOf(4).equals(order.getStatus())) {
            return order;
        }

        if (!Integer.valueOf(3).equals(order.getStatus())) {
            throw conflict("仅维修中的工单可以提交维修结果");
        }

        String repairResult = requireText(
                dto.getRepairResult(), "维修结果说明", 2000);

        List<RepairAttachment> attachments = requireAttachments(
                workspaceId,
                operator.getId(),
                dto.getImageIds(),
                2,
                true);

        LocalDateTime now = LocalDateTime.now();

        order.setRepairResult(repairResult);
        order.setStatus(4);
        order.setUpdateTime(now);

        saveOrderOrThrow(order);

        RepairProcessRecord process = createProcessRecord(
                order,
                operator.getId(),
                "SUBMIT",
                3,
                4,
                repairResult,
                now);

        // 本次维修后图片关联本次 SUBMIT 记录。
        bindAttachments(order, process, attachments, 2, now);

        // completedTime 在报修人确认完成时设置。
        return order;
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

    private RepairUser requireUser(Long workspaceId, Long userId) {
        checkId(workspaceId, "工作空间ID");
        checkId(userId, "操作人ID");

        RepairUser user = repairUserMapper.selectOne(
                new LambdaQueryWrapper<RepairUser>()
                        .eq(RepairUser::getWorkspaceId, workspaceId)
                        .eq(RepairUser::getId, userId));

        if (user == null) {
            throw notFound("模拟用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw forbidden("模拟用户已停用");
        }
        if (!"REPORTER".equals(user.getRole())
                && !"MAINTAINER".equals(user.getRole())
                && !"ADMIN".equals(user.getRole())) {
            throw forbidden("模拟用户角色异常");
        }
        return user;
    }

    private RepairOrder requireOrder(Long workspaceId, Long orderId, boolean forUpdate) {
        checkId(orderId, "工单ID");
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairOrder::getWorkspaceId, workspaceId)
                .eq(RepairOrder::getId, orderId);
        if (forUpdate) {
            wrapper.last("FOR UPDATE");
        }
        RepairOrder order = baseMapper.selectOne(wrapper);
        if (order == null) {
            throw notFound("工单不存在");
        }
        return order;
    }

    private void checkViewPermission(Long workspaceId, RepairOrder order, RepairUser operator) {
        if ("ADMIN".equals(operator.getRole())) {
            return;
        }
        if ("REPORTER".equals(operator.getRole()) && Objects.equals(order.getReporterId(), operator.getId())) {
            return;
        }
        if ("MAINTAINER".equals(operator.getRole())) {
            if (Objects.equals(order.getMaintainerId(), operator.getId())) {
                return;
            }
            Long count = repairProcessRecordMapper.selectCount(
                    new LambdaQueryWrapper<RepairProcessRecord>()
                            .eq(RepairProcessRecord::getWorkspaceId, workspaceId)
                            .eq(RepairProcessRecord::getOrderId, order.getId())
                            .and(w -> w
                                    .eq(RepairProcessRecord::getOperatorId,
                                            operator.getId())
                                    .or()
                                    .eq(RepairProcessRecord::getTargetUserId,
                                            operator.getId())));
            if (count > 0) {
                return;
            }
        }

        throw forbidden("无权查看此工单");
    }

    private List<Long> findParticipatedOrderIds(Long workspaceId, Long userId) {

        List<RepairProcessRecord> records =
                repairProcessRecordMapper.selectList(
                        new LambdaQueryWrapper<RepairProcessRecord>()
                                .select(RepairProcessRecord::getOrderId)
                                .eq(RepairProcessRecord::getWorkspaceId,
                                        workspaceId)
                                .and(w -> w
                                        .eq(RepairProcessRecord::getOperatorId,
                                                userId)
                                        .or()
                                        .eq(RepairProcessRecord::getTargetUserId,
                                                userId)));
        return records.stream()
                .map(RepairProcessRecord::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String findUserName(Long workspaceId, Long userId) {
        if (userId == null) {
            return null;
        }
        RepairUser user = repairUserMapper.selectOne(
                new LambdaQueryWrapper<RepairUser>()
                        .eq(RepairUser::getWorkspaceId, workspaceId)
                        .eq(RepairUser::getId, userId));
        return user == null ? null : user.getRealName();
    }

    /**
     * 保留原创建工单调用。
     */
    private List<RepairAttachment> requireFaultAttachments(
            Long workspaceId,
            Long userId,
            List<Long> imageIds) {

        return requireAttachments(workspaceId, userId, imageIds, 1, false);
    }

    /**
     * 保留原创建工单调用。
     */
    private void bindFaultAttachments(
            RepairOrder order,
            RepairProcessRecord process,
            List<RepairAttachment> attachments,
            LocalDateTime now) {

        bindAttachments(order, process, attachments, 1, now);
    }

    /**
     * 通用图片校验：
     * imageType：1故障图片，2维修图片；
     * required：是否必须提供图片。
     */
    private List<RepairAttachment> requireAttachments(
            Long workspaceId,
            Long userId,
            List<Long> imageIds,
            int imageType,
            boolean required) {

        if (imageIds == null || imageIds.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("请提供1～6张维修后图片");
            }
            return Collections.emptyList();
        }

        if (imageIds.size() > 6) {
            throw new IllegalArgumentException("每次最多关联6张图片");
        }

        Set<Long> uniqueIds = new HashSet<>();
        for (Long imageId : imageIds) {
            checkId(imageId, "图片ID");
            if (!uniqueIds.add(imageId)) {
                throw new IllegalArgumentException("图片ID不能重复");
            }
        }

        List<RepairAttachment> attachments = repairAttachmentMapper.selectList(
                new LambdaQueryWrapper<RepairAttachment>()
                        .eq(RepairAttachment::getWorkspaceId, workspaceId)
                        .in(RepairAttachment::getId, imageIds)
                        .orderByAsc(RepairAttachment::getId)
                        .last("FOR UPDATE"));

        if (attachments.size() != imageIds.size()) {
            throw notFound("图片不存在或不属于当前工作空间");
        }

        Map<Long, RepairAttachment> attachmentMap = new HashMap<>();

        for (RepairAttachment attachment : attachments) {
            if (!Objects.equals(attachment.getUploaderId(), userId)) {
                throw forbidden("只能关联本人上传的图片");
            }

            if (!Integer.valueOf(imageType).equals(attachment.getImageType())) {
                throw new IllegalArgumentException(
                        imageType == 1
                                ? "报修只能关联故障图片"
                                : "维修只能关联维修图片");
            }

            if (!Integer.valueOf(0).equals(attachment.getStatus())) {
                throw conflict("图片已经关联，不能重复使用");
            }

            if (attachment.getImageUrl() == null
                    || attachment.getImageUrl().isBlank()) {
                throw conflict("图片存储信息异常");
            }

            attachmentMap.put(attachment.getId(), attachment);
        }

        // 保留前端提交的图片顺序。
        return imageIds.stream()
                .map(attachmentMap::get)
                .toList();
    }

    /**
     * 通用图片绑定。
     */
    private void bindAttachments(
            RepairOrder order,
            RepairProcessRecord process,
            List<RepairAttachment> attachments,
            int imageType,
            LocalDateTime now) {

        int sortOrder = 1;

        for (RepairAttachment attachment : attachments) {
            int updated = repairAttachmentMapper.update(
                    null,
                    new LambdaUpdateWrapper<RepairAttachment>()
                            .eq(RepairAttachment::getWorkspaceId,
                                    order.getWorkspaceId())
                            .eq(RepairAttachment::getId, attachment.getId())
                            .eq(RepairAttachment::getUploaderId,
                                    process.getOperatorId())
                            .eq(RepairAttachment::getImageType, imageType)
                            .eq(RepairAttachment::getStatus, 0)
                            .set(RepairAttachment::getStatus, 1)
                            .set(RepairAttachment::getUpdateTime, now));

            if (updated != 1) {
                throw conflict("图片状态已变化，请重新提交");
            }

            RepairOrderImage image = new RepairOrderImage();
            image.setWorkspaceId(order.getWorkspaceId());
            image.setOrderId(order.getId());
            image.setAttachmentId(attachment.getId());
            image.setImageUrl(attachment.getImageUrl());
            image.setImageType(imageType);
            image.setSortOrder(sortOrder++);
            image.setUploaderId(attachment.getUploaderId());
            image.setProcessRecordId(process.getId());
            image.setCreateTime(now);
            image.setUpdateTime(now);

            if (repairOrderImageMapper.insert(image) != 1) {
                throw new IllegalStateException("关联工单图片失败");
            }
        }
    }

    /**
     * 普通处理记录，无分派目标。
     */
    private RepairProcessRecord createProcessRecord(
            RepairOrder order,
            Long operatorId,
            String action,
            Integer fromStatus,
            Integer toStatus,
            String content,
            LocalDateTime now) {

        return createProcessRecord(
                order,
                operatorId,
                action,
                fromStatus,
                toStatus,
                null,
                content,
                now);
    }

    /**
     * 支持保存分派目标，便于查询师傅曾参与的工单。
     */
    private RepairProcessRecord createProcessRecord(
            RepairOrder order,
            Long operatorId,
            String action,
            Integer fromStatus,
            Integer toStatus,
            Long targetUserId,
            String content,
            LocalDateTime now) {

        RepairProcessRecord record = new RepairProcessRecord();
        record.setWorkspaceId(order.getWorkspaceId());
        record.setOrderId(order.getId());
        record.setOperatorId(operatorId);
        record.setAction(action);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setTargetUserId(targetUserId);
        record.setContent(content);
        record.setCreateTime(now);
        record.setUpdateTime(now);

        if (repairProcessRecordMapper.insert(record) != 1) {
            throw new IllegalStateException("保存工单处理记录失败");
        }

        if (record.getId() == null) {
            throw new IllegalStateException("工单处理记录主键未回填");
        }

        return record;
    }

    private String generateOrderNo() {
        return "BX" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private void checkCampus(String campus) {
        if (!"新吴校区".equals(campus) && !"藕塘校区".equals(campus)) {
            throw new IllegalArgumentException("校区只能为新吴校区或藕塘校区");
        }
    }

    private void checkId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "必须为正整数");
        }
    }

    private String requireText(String value, String fieldName, int maxLength) {
        String result = optionalText(value, fieldName, maxLength);
        if (result == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return result;
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

    private void checkAssignedMaintainer(RepairOrder order, Long operatorId) {
        if (!Objects.equals(order.getMaintainerId(), operatorId)) {
            throw forbidden("仅当前被分派的维修师傅可以操作");
        }
    }

    private void saveOrderOrThrow(RepairOrder order) {
        if (baseMapper.updateById(order) != 1) {
            throw new IllegalStateException("更新工单失败");
        }
    }
}
