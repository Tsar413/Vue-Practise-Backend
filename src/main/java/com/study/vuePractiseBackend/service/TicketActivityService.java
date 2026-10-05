package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.TicketActivityDTO;
import com.study.vuePractiseBackend.dto.TicketActivityStatusDTO;
import com.study.vuePractiseBackend.dto.TicketActivityUpdateDTO;
import com.study.vuePractiseBackend.entity.TicketActivity;

public interface TicketActivityService extends IService<TicketActivity> {
    TicketActivity saveNewActivity(Long workspaceId, TicketActivityDTO ticketActivityDTO);

    TicketActivity updateActivity(Long workspaceId, Long activityId, TicketActivityUpdateDTO dto);

    TicketActivity changeActivityStatus(Long workspaceId, Long activityId, TicketActivityStatusDTO dto);

    Integer deleteActivity(Long workspaceId, Long activityId, Long operatorId);

}
