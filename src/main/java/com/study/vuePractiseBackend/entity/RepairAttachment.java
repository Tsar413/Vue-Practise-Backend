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
@Table(name = "repair_attachment")
@TableName("repair_attachment")
@Data
public class RepairAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    @Column(name = "uploader_id", nullable = false)
    @TableField("uploader_id")
    private Long uploaderId;

    /** 1故障图片，2维修图片 */
    @Column(name = "image_type", nullable = false)
    @TableField("image_type")
    private Integer imageType;

    /** 服务器存储根目录下的相对路径 */
    @Column(name = "image_url", nullable = false, length = 500)
    @TableField("image_url")
    private String imageUrl;

    @Column(name = "original_name", nullable = false, length = 255)
    @TableField("original_name")
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    @TableField("content_type")
    private String contentType;

    @Column(name = "file_size", nullable = false)
    @TableField("file_size")
    private Long fileSize;

    /** 0临时未关联，1已关联 */
    @Column(nullable = false)
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}