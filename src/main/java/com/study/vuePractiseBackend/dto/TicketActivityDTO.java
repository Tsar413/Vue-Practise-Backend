package com.study.vuePractiseBackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketActivityDTO {
    private Long operatorId;          // 模拟管理员ID
    private String activityName;
    private String description;
    private String coverUrl;
    private String campus;
    private String location;
    private Integer quota;
    private LocalDateTime bookingStartTime;
    private LocalDateTime bookingEndTime;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
}
