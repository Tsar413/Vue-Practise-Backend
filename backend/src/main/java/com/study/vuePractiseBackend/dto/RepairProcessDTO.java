package com.study.vuePractiseBackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RepairProcessDTO {
    private Long operatorId;
    private String content;
    private List<Long> imageIds;
}