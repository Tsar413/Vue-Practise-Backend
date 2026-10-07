package com.study.vuePractiseBackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_user")
@TableName("sys_user")
@Data
public class SysUser {
    @Id
    @Column(name = "id", nullable = false, length = 50)
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @Column(length = 50)
    private String username;

    @Column(name = "real_name", nullable = false, length = 50)
    @TableField("real_name")
    private String realName;

    @Column(name = "class_id", length = 50)
    @TableField("class_id")
    private String classId;

    @Column(nullable = false, length = 20)
    private String role;

    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_hash", length = 255)
    @TableField("password_hash")
    private String passwordHash;

    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_salt", length = 64)
    @TableField("password_salt")
    private String passwordSalt;

    @JsonIgnore
    @Column(name = "password_algorithm", length = 40)
    @TableField("password_algorithm")
    private String passwordAlgorithm;

    @Column(nullable = false)
    private Integer status;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "last_login_time")
    @TableField(value = "last_login_time")
    private LocalDateTime lastLoginTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "create_time")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "update_time")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
