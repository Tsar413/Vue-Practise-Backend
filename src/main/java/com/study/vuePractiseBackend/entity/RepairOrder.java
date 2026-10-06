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
@Table(name = "repair_order")
@TableName("repair_order")
@Data
public class RepairOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    /** 工单编号，同一个workspace内唯一 */
    @Column(name = "order_no", nullable = false, length = 64)
    @TableField("order_no")
    private String orderNo;

    /** 对应repair_device.id；未登记设备允许为空 */
    @Column(name = "device_id")
    @TableField("device_id")
    private Long deviceId;

    @Column(name = "device_name", nullable = false, length = 100)
    @TableField("device_name")
    private String deviceName;

    @Column(name = "device_type", nullable = false, length = 50)
    @TableField("device_type")
    private String deviceType;

    /** 报修时的设备位置 */
    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false, length = 100)
    private String title;

    /** 故障详细描述 */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** 报修人，对应repair_user.id */
    @Column(name = "reporter_id", nullable = false)
    @TableField("reporter_id")
    private Long reporterId;

    /** 本次报修的联系电话 */
    @Column(name = "contact_phone", length = 20)
    @TableField("contact_phone")
    private String contactPhone;

    /** 当前维修人员，对应repair_user.id；未分派时为空 */
    @Column(name = "maintainer_id")
    @TableField("maintainer_id")
    private Long maintainerId;

    /**
     * 0已撤销
     * 1待分派
     * 2待接单
     * 3维修中
     * 4待确认
     * 5已完成
     */
    @Column(nullable = false)
    private Integer status;

    /** 报修时的校区，历史数据允许暂时为空 */
    @Column(length = 50)
    private String campus;

    /** 最近一次提交的维修结果，历史过程另存处理记录表 */
    @Column(name = "repair_result", columnDefinition = "TEXT")
    @TableField("repair_result")
    private String repairResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "completed_time", columnDefinition = "DATETIME")
    @TableField("completed_time")
    private LocalDateTime completedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "cancel_time", columnDefinition = "DATETIME")
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
