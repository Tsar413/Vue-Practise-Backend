package com.study.vuePractiseBackend.vo;

import com.study.vuePractiseBackend.entity.RepairOrder;
import lombok.Data;

@Data
public class RepairOrderDetailVO {

    private RepairOrder order;

    private String reporterName;

    private String maintainerName;
}