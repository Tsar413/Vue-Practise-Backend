package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.RepairDeviceDTO;
import com.study.vuePractiseBackend.entity.RepairDevice;

public interface RepairDeviceService extends IService<RepairDevice> {
    RepairDevice createDevice(Long workspaceId, RepairDeviceDTO dto);

    RepairDevice updateDevice(Long workspaceId, Long deviceId, RepairDeviceDTO dto);

    RepairDevice changeDeviceStatus(Long workspaceId, Long deviceId, Long operatorId, Integer status);
}
