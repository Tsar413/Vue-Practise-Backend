package com.study.vuePractiseBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.dto.*;
import com.study.vuePractiseBackend.entity.RepairDevice;
import com.study.vuePractiseBackend.entity.RepairOrder;
import com.study.vuePractiseBackend.entity.RepairProcessRecord;
import com.study.vuePractiseBackend.entity.RepairUser;
import com.study.vuePractiseBackend.entity.RepairEvaluation;
import com.study.vuePractiseBackend.service.RepairAttachmentService;
import com.study.vuePractiseBackend.vo.RepairImageVO;
import com.study.vuePractiseBackend.vo.RepairImageResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import com.study.vuePractiseBackend.interceptor.ApiAccessInterceptor;
import com.study.vuePractiseBackend.service.RepairDeviceService;
import com.study.vuePractiseBackend.service.RepairOrderService;
import com.study.vuePractiseBackend.service.RepairUserService;
import com.study.vuePractiseBackend.vo.RepairOrderDetailVO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/practice/{accessCode}/repair")
public class RepairController {

    @Resource
    private RepairUserService repairUserService;

    @Resource
    private RepairDeviceService repairDeviceService;

    @Resource
    private RepairOrderService repairOrderService;

    @jakarta.annotation.Resource
    private RepairAttachmentService repairAttachmentService;

    @PutMapping("/orders/{orderId}/confirm")
    public ResponseEntity<Result<RepairOrder>> confirmOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestParam("operatorId") Long operatorId) {
        return ResponseEntity.ok(Result.success(repairOrderService.confirmOrder(workspaceId, orderId, operatorId)));
    }

    @PutMapping("/orders/{orderId}/return")
    public ResponseEntity<Result<RepairOrder>> returnOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestBody RepairReturnDTO dto) {
        return ResponseEntity.ok(Result.success(repairOrderService.returnOrder(workspaceId, orderId, dto)));
    }

    @PostMapping("/orders/{orderId}/evaluation")
    public ResponseEntity<Result<RepairEvaluation>> evaluateOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestBody RepairEvaluationDTO dto) {
        return ResponseEntity.ok(Result.success(repairOrderService.evaluateOrder(workspaceId, orderId, dto)));
    }

    @GetMapping("/orders/{orderId}/evaluation")
    public ResponseEntity<Result<RepairEvaluation>> getEvaluation(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestParam("operatorId") Long operatorId) {
        return ResponseEntity.ok(Result.success(repairOrderService.getEvaluation(workspaceId, orderId, operatorId)));
    }

    @GetMapping("/orders/{orderId}/process-records")
    public ResponseEntity<Result<List<RepairProcessRecord>>> getProcessRecords(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestParam("operatorId") Long operatorId) {
        return ResponseEntity.ok(Result.success(repairOrderService.getProcessRecords(workspaceId, orderId, operatorId)));
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<RepairImageVO>> uploadImage(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam("operatorId") Long operatorId,
            @RequestParam("imageType") Integer imageType,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(Result.success(repairAttachmentService.upload(workspaceId, operatorId, imageType, file)));
    }

    @GetMapping("/orders/{orderId}/images")
    public ResponseEntity<Result<List<RepairImageVO>>> getOrderImages(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestParam("operatorId") Long operatorId,
            @RequestParam(value = "imageType", required = false) Integer imageType) {
        return ResponseEntity.ok(Result.success(repairAttachmentService.getOrderImages(workspaceId, orderId, operatorId, imageType)));
    }

    @GetMapping("/images/{imageId}/preview")
    public ResponseEntity<org.springframework.core.io.Resource> previewImage(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("imageId") Long imageId) {
        return imageResponse(repairAttachmentService.getImage(workspaceId, imageId), false);
    }

    @GetMapping("/images/{imageId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadImage(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("imageId") Long imageId) {
        return imageResponse(repairAttachmentService.getImage(workspaceId, imageId), true);
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Result<Integer>> deleteTemporaryImage(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("imageId") Long imageId,
            @RequestParam("operatorId") Long operatorId) {
        return ResponseEntity.ok(Result.success(repairAttachmentService.deleteTemporaryImage(workspaceId, imageId, operatorId)));
    }

    private ResponseEntity<org.springframework.core.io.Resource> imageResponse(RepairImageResource file, boolean download) {
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    @GetMapping("/users")
    public ResponseEntity<Result<List<RepairUser>>> getAllUsers(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "department", required = false) String department) {
        // role不传或为空白时，不筛选角色。
        if (role != null) {
            role = role.trim().toUpperCase(Locale.ROOT);
            if (!role.isEmpty() && !"REPORTER".equals(role) && !"ADMIN".equals(role) && !"MAINTAINER".equals(role)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Result<>(400, "角色只能为REPORTER,ADMIN或MAINTAINER", null));
            }
        }
        if (status != null && status != 0 && status != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "状态只能为0或1", null));
        }
        if (department != null && !department.isEmpty()) {
            department = department.trim();
        }
        LambdaQueryWrapper<RepairUser> wrapper = new LambdaQueryWrapper<>();
        // 必须保留：所有查询限定在当前访问码对应的空间。
        wrapper.eq(RepairUser::getWorkspaceId, workspaceId);
        if (role != null && !role.isEmpty()) {
            wrapper.eq(RepairUser::getRole, role);
        }
        if (status != null) {
            wrapper.eq(RepairUser::getStatus, status);
        }
        if (department != null && !department.isEmpty()) {
            wrapper.eq(RepairUser::getDepartment, department);
        }
        wrapper.orderByAsc(RepairUser::getUserNo);
        return ResponseEntity.ok(Result.success(repairUserService.list(wrapper)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Result<RepairUser>> getOneUser(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("userId") Long userId) {
        RepairUser user = repairUserService.getOne(
                new LambdaQueryWrapper<RepairUser>()
                        .eq(RepairUser::getWorkspaceId, workspaceId)
                        .eq(RepairUser::getId, userId));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "用户不存在", null));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    @PostMapping("/users/switch")
    public ResponseEntity<Result<RepairUser>> switchUser(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam("userNo") String userNo) {
        RepairUser user = repairUserService.switchUser(workspaceId, userNo);
        return ResponseEntity.ok(Result.success(user));
    }

    @PostMapping("/users")
    public ResponseEntity<Result<RepairUser>> createUser(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestBody RepairUserDTO dto) {
        RepairUser user = repairUserService.createUser(workspaceId, dto);
        return ResponseEntity.ok(Result.success(user));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<Result<RepairUser>> updateUser(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("userId") Long userId,
            @RequestBody RepairUserDTO dto) {
        RepairUser user = repairUserService.updateUser(workspaceId, userId, dto);
        return ResponseEntity.ok(Result.success(user));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Result<Integer>> deleteUser(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("userId") Long userId,
            @RequestParam("operatorId") Long operatorId) {
        Integer result = repairUserService.deleteUser(workspaceId, userId, operatorId);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/devices")
    public ResponseEntity<Result<List<RepairDevice>>> getAllDevices(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam(value = "campus", required = false) String campus,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "deviceType", required = false) String deviceType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        if (status != null && status != 0 && status != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "状态只能为0或1", null));
        }
        if (campus != null) {
            campus = campus.trim();
            if (!campus.isEmpty() && !"新吴校区".equals(campus) && !"藕塘校区".equals(campus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Result<>(400, "校区只能为新吴校区或藕塘校区", null));
            }
        }
        if (deviceType != null && !deviceType.isEmpty()) {
            deviceType = deviceType.trim();
        }
        if (keyword != null && !keyword.isEmpty()) {
            keyword = keyword.trim();
        }
        LambdaQueryWrapper<RepairDevice> wrapper = new LambdaQueryWrapper<>();
        // 必须保留：所有查询限定在当前访问码对应的空间。
        wrapper.eq(RepairDevice::getWorkspaceId, workspaceId);
        if (campus != null && !campus.isEmpty()) {
            wrapper.eq(RepairDevice::getCampus, campus);
        }
        if (status != null) {
            wrapper.eq(RepairDevice::getStatus, status);
        }
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(RepairDevice::getDeviceType, deviceType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            String searchKeyword = keyword;
            wrapper.and(w -> w
                    .like(RepairDevice::getDeviceNo, searchKeyword)
                    .or()
                    .like(RepairDevice::getDeviceName, searchKeyword));
        }
        wrapper.orderByAsc(RepairDevice::getDeviceNo);
        return ResponseEntity.ok(Result.success(repairDeviceService.list(wrapper)));
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<Result<RepairDevice>> getOneDevice(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("deviceId") Long deviceId) {
        RepairDevice device = repairDeviceService.getOne(
                new LambdaQueryWrapper<RepairDevice>()
                        .eq(RepairDevice::getWorkspaceId, workspaceId)
                        .eq(RepairDevice::getId, deviceId));
        if (device == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "设备不存在", null));
        }
        return ResponseEntity.ok(Result.success(device));
    }

    @PostMapping("/devices")
    public ResponseEntity<Result<RepairDevice>> createDevice(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestBody RepairDeviceDTO dto) {
        RepairDevice device = repairDeviceService.createDevice(workspaceId, dto);
        return ResponseEntity.ok(Result.success(device));
    }

    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<Result<RepairDevice>> updateDevice(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("deviceId") Long deviceId,
            @RequestBody RepairDeviceDTO dto) {
        RepairDevice device = repairDeviceService.updateDevice(workspaceId, deviceId, dto);
        return ResponseEntity.ok(Result.success(device));
    }

    @PutMapping("/devices/{deviceId}/status")
    public ResponseEntity<Result<RepairDevice>> changeDeviceStatus(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("deviceId") Long deviceId,
            @RequestParam("operatorId") Long operatorId,
            @RequestParam("status") Integer status) {
        RepairDevice device = repairDeviceService.changeDeviceStatus(workspaceId, deviceId, operatorId, status);
        return ResponseEntity.ok(Result.success(device));
    }

    @GetMapping("/orders")
    public ResponseEntity<Result<List<RepairOrder>>> getOrders(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam("operatorId") Long operatorId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "campus", required = false) String campus,
            @RequestParam(value = "deviceType", required = false) String deviceType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "scope", required = false) String scope) {
        List<RepairOrder> orders = repairOrderService.getOrders(
                workspaceId, operatorId, status,
                campus, deviceType, keyword, scope);
        return ResponseEntity.ok(Result.success(orders));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Result<RepairOrderDetailVO>> getOrderDetail(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestParam("operatorId") Long operatorId) {
        RepairOrderDetailVO result = repairOrderService.getOrderDetail(workspaceId, orderId, operatorId);
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/orders")
    public ResponseEntity<Result<RepairOrder>> createOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestBody RepairOrderCreateDTO dto) {
        RepairOrder order = repairOrderService.createOrder(workspaceId, dto);
        return ResponseEntity.ok(Result.success(order));
    }

    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Result<RepairOrder>> cancelOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestParam("operatorId") Long operatorId) {
        RepairOrder order = repairOrderService.cancelOrder(workspaceId, orderId, operatorId);
        return ResponseEntity.ok(Result.success(order));
    }

    @PutMapping("/orders/{orderId}/assign")
    public ResponseEntity<Result<RepairOrder>> assignOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestBody RepairAssignDTO dto) {
        return ResponseEntity.ok(Result.success(
                repairOrderService.assignOrder(workspaceId, orderId, dto)));
    }

    @PutMapping("/orders/{orderId}/accept")
    public ResponseEntity<Result<RepairOrder>> acceptOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestParam("operatorId") Long operatorId) {
        return ResponseEntity.ok(Result.success(
                repairOrderService.acceptOrder(workspaceId, orderId, operatorId)));
    }

    @PostMapping("/orders/{orderId}/process-records")
    public ResponseEntity<Result<RepairProcessRecord>> addProcessRecord(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestBody RepairProcessDTO dto) {
        return ResponseEntity.ok(Result.success(
                repairOrderService.addProcessRecord(workspaceId, orderId, dto)));
    }

    @PutMapping("/orders/{orderId}/submit")
    public ResponseEntity<Result<RepairOrder>> submitOrder(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("orderId") Long orderId,
            @RequestBody RepairSubmitDTO dto) {
        return ResponseEntity.ok(Result.success(
                repairOrderService.submitOrder(workspaceId, orderId, dto)));
    }
}
