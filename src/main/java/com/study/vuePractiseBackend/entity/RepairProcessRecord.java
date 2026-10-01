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
@Table(name = "repair_process_record")
@TableName("repair_process_record")
@Data
public class RepairProcessRecord {
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

    /** 操作人，对应repair_user.id */
    @Column(name = "operator_id", nullable = false)
    @TableField("operator_id")
    private Long operatorId;

    /**
     * CREATE创建、ASSIGN分派、ACCEPT接单、
     * RECORD记录维修情况、SUBMIT提交结果、
     * RETURN退回、CONFIRM确认完成、CANCEL撤销。
     */
    @Column(nullable = false, length = 30)
    private String action;

    /** 操作前状态，创建工单时可以为空 */
    @Column(name = "from_status")
    @TableField("from_status")
    private Integer fromStatus;

    /** 操作后状态；只追加维修说明时可以与原状态相同 */
    @Column(name = "to_status", nullable = false)
    @TableField("to_status")
    private Integer toStatus;

    /** 分派目标维修人员，对应repair_user.id；其他操作可为空 */
    @Column(name = "target_user_id")
    @TableField("target_user_id")
    private Long targetUserId;

    /** 维修说明、退回原因等 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
