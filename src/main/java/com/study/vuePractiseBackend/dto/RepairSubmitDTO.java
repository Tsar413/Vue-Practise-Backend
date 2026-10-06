package com.study.vuePractiseBackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RepairSubmitDTO {
    private Long operatorId;
    private String repairResult;
    private List<Long> imageIds;
}