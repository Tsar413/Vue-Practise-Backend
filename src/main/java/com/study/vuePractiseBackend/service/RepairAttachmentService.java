package com.study.vuePractiseBackend.service;

import com.study.vuePractiseBackend.vo.RepairImageResource;
import com.study.vuePractiseBackend.vo.RepairImageVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface RepairAttachmentService {
    RepairImageVO upload(Long workspaceId, Long operatorId, Integer imageType, MultipartFile file);
    List<RepairImageVO> getOrderImages(Long workspaceId, Long orderId, Long operatorId, Integer imageType);
    RepairImageResource getImage(Long workspaceId, Long imageId);
    Integer deleteTemporaryImage(Long workspaceId, Long imageId, Long operatorId);
}
