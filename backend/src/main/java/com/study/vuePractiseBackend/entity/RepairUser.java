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
@Table(name = "repair_user")
@TableName("repair_user")
@Data
public class RepairUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属学生工作空间 */
    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    /** 模拟用户编号，同一个workspace内不能重复 */
    @Column(name = "user_no", nullable = false, length = 50)
    @TableField("user_no")
    private String userNo;

    @Column(name = "real_name", nullable = false, length = 50)
    @TableField("real_name")
    private String realName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String department;

    /**
     * REPORTER：报修人
     * MAINTAINER：维修人员
     * ADMIN：项目管理员
     */
    @Column(nullable = false, length = 20)
    private String role;

    /** 1正常，0停用 */
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
