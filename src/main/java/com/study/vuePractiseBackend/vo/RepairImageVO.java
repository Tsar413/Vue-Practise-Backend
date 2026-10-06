package com.study.vuePractiseBackend.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RepairImageVO {
    // 附件ID：用于imageIds及预览/下载路径，不是repair_order_image.id。
    private Long id;
    private Long orderImageId;
    private Long orderId;
    private Long processRecordId;
    private Long uploaderId;
    private Integer imageType;
    private Integer status;
    private Integer sortOrder;
    private String originalName;
    private String contentType;
    private Long fileSize;
    // 相对 /api/practice/{accessCode}，前端拼接服务器地址与访问码。
    private String previewPath;
    private String downloadPath;
    private LocalDateTime createTime;
}
