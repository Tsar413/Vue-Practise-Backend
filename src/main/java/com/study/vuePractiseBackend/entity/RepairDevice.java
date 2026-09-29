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
@Table(name = "repair_device")
@TableName("repair_device")
@Data
public class RepairDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    /** 设备编号，同一个workspace内不能重复 */
    @Column(name = "device_no", nullable = false, length = 50)
    @TableField("device_no")
    private String deviceNo;

    @Column(name = "device_name", nullable = false, length = 100)
    @TableField("device_name")
    private String deviceName;

    /** 例如：电脑、投影仪、打印机 */
    @Column(name = "device_type", nullable = false, length = 50)
    @TableField("device_type")
    private String deviceType;

    @Column(length = 50)
    private String campus;

    @Column(nullable = false, length = 200)
    private String location;

    /** 1在用，0停用；不表示工单维修进度 */
    @Column(nullable = false)
    private Integer status;

    @Column(length = 500)
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
