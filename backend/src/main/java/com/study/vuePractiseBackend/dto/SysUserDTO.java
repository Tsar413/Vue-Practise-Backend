package com.study.vuePractiseBackend.dto;

import lombok.Data;

@Data
public class SysUserDTO {
    private String id;
    private String username;
    private String realName;
    private String classId;
    private String role;
}
