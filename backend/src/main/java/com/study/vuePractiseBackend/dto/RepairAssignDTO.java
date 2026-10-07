package com.study.vuePractiseBackend.dto;

import lombok.Data;

@Data
public class RepairAssignDTO {
    private Long operatorId;
    private Long maintainerId;
    private String content;
}