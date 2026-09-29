package com.study.vuePractiseBackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_record",)
@TableName("ticket_record")
@Data
public class TicketRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属学生工作空间，对应sys_workspace.id */
    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    /** 对应ticket_activity.id */
    @Column(name = "activity_id", nullable = false)
    @TableField("activity_id")
    private Long activityId;

    /** 抢票的模拟业务用户，对应ticket_user.id，不是学生学号 */
    @Column(name = "user_id", nullable = false)
    @TableField("user_id")
    private Long userId;

    /** 票券凭证号，用于展示与核销 */
    @Column(name = "ticket_no", nullable = false, length = 64)
    @TableField("ticket_no")
    private String ticketNo;

    /** 0已取消，1有效，2已核销 */
    @Column(nullable = false)
    private Integer status;

    /** 最近一次成功报名的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "booking_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("booking_time")
    private LocalDateTime bookingTime;

    /** 取消时间，尚未取消时为空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "cancel_time", columnDefinition = "DATETIME")
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /** 核销时间，尚未核销时为空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "verify_time", columnDefinition = "DATETIME")
    @TableField("verify_time")
    private LocalDateTime verifyTime;

    /** 核销管理员，对应ticket_user.id，尚未核销时为空 */
    @Column(name = "verify_user_id")
    @TableField("verify_user_id")
    private Long verifyUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}