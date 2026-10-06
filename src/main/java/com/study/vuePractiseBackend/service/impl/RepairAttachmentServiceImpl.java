package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.vuePractiseBackend.entity.*;
import com.study.vuePractiseBackend.mapper.*;
import com.study.vuePractiseBackend.service.RepairAttachmentService;
import com.study.vuePractiseBackend.service.RepairOrderService;
import com.study.vuePractiseBackend.util.RepairFileUtil;
import com.study.vuePractiseBackend.vo.RepairImageResource;
import com.study.vuePractiseBackend.vo.RepairImageVO;
import jakarta.annotation.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.*;

@Service
public class RepairAttachmentServiceImpl implements RepairAttachmentService {
    @Resource private RepairAttachmentMapper attachmentMapper;
    @Resource private RepairOrderImageMapper orderImageMapper;
    @Resource private RepairUserMapper userMapper;
    @Resource private SysWorkspaceMapper workspaceMapper;
    @Resource private RepairOrderService orderService;
    @Resource private RepairFileUtil fileUtil;
    @Resource private RepairFileCleanupService cleanupService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairImageVO upload(Long workspaceId, Long operatorId, Integer imageType, MultipartFile file) {
        lockWorkspace(workspaceId);
        RepairUser user = requireUser(workspaceId, operatorId);
        if (!Integer.valueOf(1).equals(imageType) && !Integer.valueOf(2).equals(imageType)) {
            throw new IllegalArgumentException("图片类型只能为1故障图片或2维修图片");
        }
        if (imageType == 1 && !"REPORTER".equals(user.getRole())) {
            throw forbidden("仅报修人可以上传故障图片");
        }
        if (imageType == 2 && !"MAINTAINER".equals(user.getRole())) {
            throw forbidden("仅维修师傅可以上传维修图片");
        }

        RepairFileUtil.SavedFile saved = fileUtil.save(workspaceId, file);
        // 包括提交事务失败的情况：文件保存成功、数据库回滚时删除新文件。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) fileUtil.deleteQuietly(saved.imageUrl());
            }
        });
        LocalDateTime now = LocalDateTime.now();
        RepairAttachment attachment = new RepairAttachment();
        attachment.setWorkspaceId(workspaceId);
        attachment.setUploaderId(operatorId);
        attachment.setImageType(imageType);
        attachment.setImageUrl(saved.imageUrl());
        attachment.setOriginalName(saved.originalName());
        attachment.setContentType(saved.contentType());
        attachment.setFileSize(saved.fileSize());
        attachment.setStatus(0);
        attachment.setCreateTime(now);
        attachment.setUpdateTime(now);
        if (attachmentMapper.insert(attachment) != 1 || attachment.getId() == null) {
            throw new IllegalStateException("保存图片附件失败");
        }
        return toVO(attachment);
    }

    @Override
    public List<RepairImageVO> getOrderImages(
            Long workspaceId, Long orderId, Long operatorId, Integer imageType) {
        // 复用工单详情权限：本人、管理员、当前/历史参与师傅。
        orderService.getOrderDetail(workspaceId, orderId, operatorId);
        if (imageType != null && imageType != 1 && imageType != 2) {
            throw new IllegalArgumentException("图片类型只能为1或2");
        }
        var query = new LambdaQueryWrapper<RepairOrderImage>()
                .eq(RepairOrderImage::getWorkspaceId, workspaceId)
                .eq(RepairOrderImage::getOrderId, orderId)
                .eq(imageType != null, RepairOrderImage::getImageType, imageType)
                .orderByAsc(RepairOrderImage::getProcessRecordId)
                .orderByAsc(RepairOrderImage::getSortOrder)
                .orderByAsc(RepairOrderImage::getId);
        List<RepairOrderImage> images = orderImageMapper.selectList(query);
        if (images.isEmpty()) return List.of();
        List<Long> ids = images.stream().map(RepairOrderImage::getAttachmentId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, RepairAttachment> attachments = ids.isEmpty() ? Map.of() :
                attachmentMapper.selectList(new LambdaQueryWrapper<RepairAttachment>()
                        .eq(RepairAttachment::getWorkspaceId, workspaceId)
                        .in(RepairAttachment::getId, ids))
                        .stream().collect(Collectors.toMap(RepairAttachment::getId, Function.identity()));
        List<RepairImageVO> result = new ArrayList<>();
        for (RepairOrderImage image : images) {
            RepairAttachment attachment = attachments.get(image.getAttachmentId());
            if (attachment == null) throw new IllegalStateException("工单图片关联的附件不存在");
            RepairImageVO vo = toVO(attachment);
            vo.setOrderImageId(image.getId());
            vo.setOrderId(orderId);
            vo.setProcessRecordId(image.getProcessRecordId());
            vo.setSortOrder(image.getSortOrder());
            result.add(vo);
        }
        return result;
    }

    @Override
    public RepairImageResource getImage(Long workspaceId, Long imageId) {
        checkId(workspaceId, "工作空间ID");
        checkId(imageId, "图片ID");
        RepairAttachment attachment = attachmentMapper.selectOne(
                new LambdaQueryWrapper<RepairAttachment>()
                        .eq(RepairAttachment::getWorkspaceId, workspaceId)
                        .eq(RepairAttachment::getId, imageId));
        if (attachment == null) throw notFound("图片不存在");
        String type = attachment.getContentType();
        if (!Set.of("image/jpeg", "image/png", "image/webp").contains(type == null ? "" : type)) {
            throw new IllegalStateException("图片内容类型异常");
        }
        // 读取后响应，避免重置清理与延迟打开Resource之间的竞态。
        return new RepairImageResource(new ByteArrayResource(fileUtil.read(attachment.getImageUrl())),
                type, attachment.getOriginalName());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer deleteTemporaryImage(Long workspaceId, Long imageId, Long operatorId) {
        lockWorkspace(workspaceId);
        requireUser(workspaceId, operatorId);
        checkId(imageId, "图片ID");
        RepairAttachment attachment = attachmentMapper.selectOne(
                new LambdaQueryWrapper<RepairAttachment>()
                        .eq(RepairAttachment::getWorkspaceId, workspaceId)
                        .eq(RepairAttachment::getId, imageId).last("FOR UPDATE"));
        if (attachment == null) throw notFound("图片不存在");
        if (!Objects.equals(attachment.getUploaderId(), operatorId)) {
            throw forbidden("只能删除本人上传的临时图片");
        }
        if (!Integer.valueOf(0).equals(attachment.getStatus())) {
            throw conflict("已关联工单的图片不能单独删除");
        }
        if (orderImageMapper.selectCount(new LambdaQueryWrapper<RepairOrderImage>()
                .eq(RepairOrderImage::getWorkspaceId, workspaceId)
                .eq(RepairOrderImage::getAttachmentId, imageId)) > 0) {
            throw conflict("图片已被工单引用，不能删除");
        }
        cleanupService.enqueue(List.of(attachment.getImageUrl()));
        if (attachmentMapper.delete(new LambdaQueryWrapper<RepairAttachment>()
                .eq(RepairAttachment::getWorkspaceId, workspaceId)
                .eq(RepairAttachment::getId, imageId)
                .eq(RepairAttachment::getStatus, 0)) != 1) {
            throw conflict("图片状态已变化，请重试");
        }
        return 1;
    }

    private RepairImageVO toVO(RepairAttachment attachment) {
        RepairImageVO vo = new RepairImageVO();
        vo.setId(attachment.getId());
        vo.setUploaderId(attachment.getUploaderId());
        vo.setImageType(attachment.getImageType());
        vo.setStatus(attachment.getStatus());
        vo.setOriginalName(attachment.getOriginalName());
        vo.setContentType(attachment.getContentType());
        vo.setFileSize(attachment.getFileSize());
        vo.setCreateTime(attachment.getCreateTime());
        vo.setPreviewPath("/repair/images/" + attachment.getId() + "/preview");
        vo.setDownloadPath("/repair/images/" + attachment.getId() + "/download");
        return vo;
    }

    private void lockWorkspace(Long workspaceId) {
        checkId(workspaceId, "工作空间ID");
        SysWorkspace workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<SysWorkspace>()
                .eq(SysWorkspace::getId, workspaceId).last("FOR UPDATE"));
        if (workspace == null) throw notFound("工作空间不存在");
        if (!Integer.valueOf(1).equals(workspace.getStatus())) throw forbidden("工作空间已暂停");
    }

    private RepairUser requireUser(Long workspaceId, Long operatorId) {
        checkId(operatorId, "操作人ID");
        RepairUser user = userMapper.selectOne(new LambdaQueryWrapper<RepairUser>()
                .eq(RepairUser::getWorkspaceId, workspaceId).eq(RepairUser::getId, operatorId));
        if (user == null) throw notFound("模拟用户不存在");
        if (!Integer.valueOf(1).equals(user.getStatus())) throw forbidden("模拟用户已停用");
        return user;
    }

    private void checkId(Long id, String name) {
        if (id == null || id <= 0) throw new IllegalArgumentException(name + "必须为正整数");
    }
}
