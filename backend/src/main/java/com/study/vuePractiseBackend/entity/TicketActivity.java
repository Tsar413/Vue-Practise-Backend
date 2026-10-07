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
@Table(name = "ticket_activity")
@TableName("ticket_activity")
@Data
public class TicketActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "activity_name", nullable = false, length = 100)
    @TableField("activity_name")
    private String activityName;

    /** 所属学生工作空间 */
    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    @Lob
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    @TableField("cover_url")
    private String coverUrl;

    @Column(length = 50)
    private String campus;

    @Column(length = 200)
    private String location;

    /** 0草稿，1已发布，2已关闭 */
    @Column(nullable = false)
    private Integer status;

    @Column(nullable = false)
    private Integer quota;

    @Column(name = "booked_count", nullable = false)
    @TableField("booked_count")
    private Integer bookedCount;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "booking_start_time")
    @TableField(value = "booking_start_time")
    private LocalDateTime bookingStartTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "booking_end_time")
    @TableField(value = "booking_end_time")
    private LocalDateTime bookingEndTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "activity_start_time")
    @TableField(value = "activity_start_time")
    private LocalDateTime activityStartTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "activity_end_time")
    @TableField(value = "activity_end_time")
    private LocalDateTime activityEndTime;
}
