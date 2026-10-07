package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.entity.TicketUser;

public interface TicketUserService extends IService<TicketUser> {
    TicketUser switchUser(Long workspaceId, String userNo);
}
