package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.SysLoginDTO;
import com.study.vuePractiseBackend.dto.SysLoginReturnDTO;
import com.study.vuePractiseBackend.entity.SysLoginToken;

public interface SysLoginService extends IService<SysLoginToken> {
    SysLoginReturnDTO login(SysLoginDTO sysLoginDTO);

    Integer logout(String rawToken);

    void revokeByUserId(String userId);

    void deleteByUserId(String userId);
}
