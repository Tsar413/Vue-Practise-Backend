package com.study.vuePractiseBackend.dto;

import lombok.Data;

@Data
public class RepairEvaluationDTO {
    private Long operatorId;
    private Integer score;
    private String content;
}
