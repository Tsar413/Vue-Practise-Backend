package com.study.vuePractiseBackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SysLoginReturnDTO {
    private Integer status;
    private String userId;
    private String token;
    private String apiAccessCode;
    private String realName;
    private String role;
}