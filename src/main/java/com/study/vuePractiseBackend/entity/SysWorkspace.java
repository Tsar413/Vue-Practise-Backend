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
@Table(name = "sys_workspace")
@TableName("sys_workspace")
@Data
public class SysWorkspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "student_id", unique = true, nullable = false, length = 50)
    @TableField("student_id")
    private String studentId;

    @Column(nullable = false)
    private Integer status;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "create_time")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "update_time")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
