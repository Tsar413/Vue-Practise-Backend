package com.study.vuePractiseBackend.dto;

import lombok.Data;

@Data
public class RepairUserDTO {

    private Long operatorId;     // 当前管理员数据库ID

    private String userNo;       // 创建时填写，修改时保持不变

    private String realName;

    private String phone;

    private String department;

    private String role;         // REPORTER、MAINTAINER、ADMIN

    private Integer status;      // 0停用、1启用
}