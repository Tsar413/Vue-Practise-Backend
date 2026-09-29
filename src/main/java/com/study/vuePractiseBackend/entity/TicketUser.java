package com.study.vuePractiseBackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Entity
@Table(name = "ticket_user")
@TableName("ticket_user")
@Data
public class TicketUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "user_no", length = 50, nullable = false)
    @TableField("user_no")
    private String userNo;

    @Column(name = "real_name", length = 50, nullable = false)
    @TableField("real_name")
    private String realName;

    /** 所属学生工作空间 */
    @Column(name = "workspace_id", nullable = false)
    @TableField("workspace_id")
    private Long workspaceId;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String campus;

    @Column(length = 20)
    private String role;

    @Column(nullable = false)
    private Integer status;
}
