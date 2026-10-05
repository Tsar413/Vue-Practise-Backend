package com.study.vuePractiseBackend.dto;

import lombok.Data;

@Data
public class TicketActivityStatusDTO {

    private Long operatorId;

    /** 1发布，2关闭 */
    private Integer status;
}