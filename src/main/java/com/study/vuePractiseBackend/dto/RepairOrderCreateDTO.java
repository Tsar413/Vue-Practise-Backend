package com.study.vuePractiseBackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RepairOrderCreateDTO {

    private Long operatorId;

    private Long deviceId;

    // deviceId为空时必填。
    private String deviceName;
    private String deviceType;
    private String campus;
    private String location;

    private String title;
    private String description;
    private String contactPhone;

    // repair_attachment.id。
    private List<Long> imageIds;
}