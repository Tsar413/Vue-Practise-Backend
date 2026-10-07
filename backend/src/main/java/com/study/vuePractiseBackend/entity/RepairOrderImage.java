package com.study.vuePractiseBackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_order_image")
@TableName("repair_order_image")
@Data
public class RepairOrderImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    /** 对应repair_order.id */
    @Column(name = "order_id", nullable = false)
    @TableField("order_id")
    private Long orderId;

    @Column(name = "image_url", nullable = false, length = 500)
    @TableField("image_url")
    private String imageUrl;

    /** 1故障图片，2维修结果图片 */
    @Column(name = "image_type", nullable = false)
    @TableField("image_type")
    private Integer imageType;

    /** 图片展示顺序 */
    @Column(name = "sort_order", nullable = false)
    @TableField("sort_order")
    private Integer sortOrder;

    /** 上传人，对应repair_user.id */
    @Column(name = "uploader_id", nullable = false)
    @TableField("uploader_id")
    private Long uploaderId;

    /** 对应repair_attachment.id */
    @Column(name = "attachment_id")
    @TableField("attachment_id")
    private Long attachmentId;

    /** 对应repair_process_record.id，可以为空 */
    @Column(name = "process_record_id")
    @TableField("process_record_id")
    private Long processRecordId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false,
            columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false,
            columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
