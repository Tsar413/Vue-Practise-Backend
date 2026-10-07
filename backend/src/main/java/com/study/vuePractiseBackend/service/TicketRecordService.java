package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.entity.TicketRecord;

import java.util.List;

public interface TicketRecordService extends IService<TicketRecord> {
    TicketRecord bookTicket(Long workspaceId, Long activityId, Long userId);

    TicketRecord cancelTicket(Long workspaceId, Long recordId, Long userId);

    List<TicketRecord> getUserRecords(Long workspaceId, Long userId, Integer status, Long activityId);

    List<TicketRecord> getActivityRecords(Long workspaceId, Long activityId, Long operatorId, Integer status);

    TicketRecord getRecordDetail(Long workspaceId, Long recordId, Long operatorId);

    TicketRecord verifyTicket(Long workspaceId, Long recordId, Long operatorId);
}
