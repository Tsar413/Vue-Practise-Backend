package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.RepairUserDTO;
import com.study.vuePractiseBackend.entity.RepairUser;

public interface RepairUserService extends IService<RepairUser> {
    RepairUser switchUser(Long workspaceId, String userNo);

    RepairUser createUser(Long workspaceId, RepairUserDTO dto);

    RepairUser updateUser(Long workspaceId, Long userId, RepairUserDTO dto);

    Integer deleteUser(Long workspaceId, Long userId, Long operatorId);
}
