package com.study.vuePractiseBackend.dto;

import lombok.Data;

@Data
public class RepairDeviceDTO {
    private Long operatorId;       // 模拟管理员数据库ID
    private String deviceNo;
    private String deviceName;
    private String deviceType;
    private String campus;
    private String location;
    private String remark;
}
