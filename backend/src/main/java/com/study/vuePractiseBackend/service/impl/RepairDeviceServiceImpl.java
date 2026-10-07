package com.study.vuePractiseBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.vuePractiseBackend.dto.RepairDeviceDTO;
import com.study.vuePractiseBackend.entity.RepairDevice;
import com.study.vuePractiseBackend.entity.RepairUser;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.mapper.RepairDeviceMapper;
import com.study.vuePractiseBackend.mapper.RepairUserMapper;
import com.study.vuePractiseBackend.mapper.SysWorkspaceMapper;
import com.study.vuePractiseBackend.service.RepairDeviceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.study.vuePractiseBackend.exception.BusinessExceptions.conflict;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.forbidden;
import static com.study.vuePractiseBackend.exception.BusinessExceptions.notFound;

@Service
public class RepairDeviceServiceImpl extends ServiceImpl<RepairDeviceMapper, RepairDevice> implements RepairDeviceService {

    @Resource
    private RepairUserMapper repairUserMapper;

    @Resource
    private SysWorkspaceMapper sysWorkspaceMapper;

    /**
     * 新建设备，默认启用。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairDevice createDevice(Long workspaceId, RepairDeviceDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }
        lockWorkspace(workspaceId);
        checkAdmin(workspaceId, dto.getOperatorId());

        RepairDevice device = new RepairDevice();
        copyValidatedFields(device, dto);

        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<RepairDevice>()
                        .eq(RepairDevice::getWorkspaceId, workspaceId)
                        .eq(RepairDevice::getDeviceNo, device.getDeviceNo()));
        if (count > 0) {
            throw conflict("当前工作空间内设备编号已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        device.setWorkspaceId(workspaceId);
        device.setStatus(1);
        device.setCreateTime(now);
        device.setUpdateTime(now);
        if (baseMapper.insert(device) != 1) {
            throw new IllegalStateException("新建设备失败");
        }
        return device;
    }

    /**
     * 修改设备，编号和状态保持不变。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairDevice updateDevice(Long workspaceId, Long deviceId, RepairDeviceDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }
        checkId(deviceId, "设备ID");
        lockWorkspace(workspaceId);
        checkAdmin(workspaceId, dto.getOperatorId());
        RepairDevice device = findDevice(workspaceId, deviceId);
        String deviceNo = requireText(dto.getDeviceNo(), "设备编号", 50);
        if (!device.getDeviceNo().equals(deviceNo)) {
            throw new IllegalArgumentException("设备编号不允许修改");
        }

        copyValidatedFields(device, dto);
        device.setUpdateTime(LocalDateTime.now());

        // 显式set，允许remark为null时清空备注。
        LambdaUpdateWrapper<RepairDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairDevice::getWorkspaceId, workspaceId)
                .eq(RepairDevice::getId, deviceId)
                .set(RepairDevice::getDeviceName, device.getDeviceName())
                .set(RepairDevice::getDeviceType, device.getDeviceType())
                .set(RepairDevice::getCampus, device.getCampus())
                .set(RepairDevice::getLocation, device.getLocation())
                .set(RepairDevice::getRemark, device.getRemark())
                .set(RepairDevice::getUpdateTime, device.getUpdateTime());
        if (baseMapper.update(null, wrapper) != 1) {
            throw new IllegalStateException("修改设备失败");
        }

        return device;
    }

    /**
     * 启用或停用设备。
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RepairDevice changeDeviceStatus(Long workspaceId, Long deviceId, Long operatorId, Integer status) {
        checkId(deviceId, "设备ID");
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态只能为0或1");
        }
        lockWorkspace(workspaceId);
        checkAdmin(workspaceId, operatorId);
        RepairDevice device = findDevice(workspaceId, deviceId);
        if (status.equals(device.getStatus())) {
            return device;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairDevice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairDevice::getWorkspaceId, workspaceId)
                .eq(RepairDevice::getId, deviceId)
                .set(RepairDevice::getStatus, status)
                .set(RepairDevice::getUpdateTime, now);
        if (baseMapper.update(null, wrapper) != 1) {
            throw new IllegalStateException("修改设备状态失败");
        }
        device.setStatus(status);
        device.setUpdateTime(now);
        return device;
    }

    /**
     * 与用户管理、初始化和重置共用工作空间行锁。
     */
    private void lockWorkspace(Long workspaceId) {
        checkId(workspaceId, "工作空间ID");
        SysWorkspace workspace = sysWorkspaceMapper.selectOne(
                new LambdaQueryWrapper<SysWorkspace>()
                        .eq(SysWorkspace::getId, workspaceId)
                        .last("FOR UPDATE"));
        if (workspace == null) {
            throw notFound("工作空间不存在");
        }
        if (!Integer.valueOf(1).equals(workspace.getStatus())) {
            throw forbidden("工作空间已暂停");
        }
    }

    private void checkAdmin(Long workspaceId, Long operatorId) {
        checkId(operatorId, "操作人ID");
        RepairUser operator = repairUserMapper.selectOne(
                new LambdaQueryWrapper<RepairUser>()
                        .eq(RepairUser::getWorkspaceId, workspaceId)
                        .eq(RepairUser::getId, operatorId));
        if (operator == null) {
            throw notFound("模拟用户不存在");
        }
        if (!Integer.valueOf(1).equals(operator.getStatus())) {
            throw forbidden("模拟用户已停用");
        }
        if (!"ADMIN".equals(operator.getRole())) {
            throw forbidden("仅管理员可以管理设备");
        }
    }

    private RepairDevice findDevice(Long workspaceId, Long deviceId) {
        RepairDevice device = baseMapper.selectOne(
                new LambdaQueryWrapper<RepairDevice>()
                        .eq(RepairDevice::getWorkspaceId, workspaceId)
                        .eq(RepairDevice::getId, deviceId));
        if (device == null) {
            throw notFound("设备不存在");
        }
        return device;
    }

    private void copyValidatedFields(RepairDevice device, RepairDeviceDTO dto) {
        String deviceNo = requireText(dto.getDeviceNo(), "设备编号", 50);
        String deviceName = requireText(dto.getDeviceName(), "设备名称", 100);
        // 不限制固定类型清单。
        String deviceType = requireText(dto.getDeviceType(), "设备类型", 50);
        String campus = requireText(dto.getCampus(), "校区", 50);
        if (!"新吴校区".equals(campus) && !"藕塘校区".equals(campus)) {
            throw new IllegalArgumentException("校区只能为新吴校区或藕塘校区");
        }
        String location = requireText(dto.getLocation(), "设备地点", 200);
        String remark = dto.getRemark();
        if (remark != null) {
            remark = remark.trim();
            if (remark.length() > 500) {
                throw new IllegalArgumentException("备注不能超过500个字符");
            }
            if (remark.isEmpty()) {
                remark = null;
            }
        }
        device.setDeviceNo(deviceNo);
        device.setDeviceName(deviceName);
        device.setDeviceType(deviceType);
        device.setCampus(campus);
        device.setLocation(location);
        device.setRemark(remark);
    }

    private void checkId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "必须为正整数");
        }
    }

    private String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        value = value.trim();
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "不能超过" + maxLength + "个字符");
        }
        return value;
    }
}