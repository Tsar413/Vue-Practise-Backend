package com.study.vuePractiseBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.dto.TicketActivityDTO;
import com.study.vuePractiseBackend.dto.TicketActivityStatusDTO;
import com.study.vuePractiseBackend.entity.TicketActivity;
import com.study.vuePractiseBackend.entity.TicketRecord;
import com.study.vuePractiseBackend.entity.TicketUser;
import com.study.vuePractiseBackend.interceptor.ApiAccessInterceptor;
import com.study.vuePractiseBackend.mapper.TickerUserMapper;
import com.study.vuePractiseBackend.service.TicketActivityService;
import com.study.vuePractiseBackend.service.TicketRecordService;
import com.study.vuePractiseBackend.service.TicketUserService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/practice/{accessCode}/ticket")
public class TicketController {

    @Resource
    private TicketUserService ticketUserService;

    @Resource
    private TicketActivityService ticketActivityService;

    @Resource
    private TicketRecordService ticketRecordService;

    @GetMapping("/users")
    public ResponseEntity<Result<List<TicketUser>>> getAllUsers(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) Integer status) {
        // role不传或为空白时，不筛选角色。
        if (role != null) {
            role = role.trim().toUpperCase(Locale.ROOT);
            if (!role.isEmpty() && !"USER".equals(role) && !"ADMIN".equals(role)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Result<>(400, "角色只能为USER或ADMIN", null));
            }
        }
        if (status != null && status != 0 && status != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "状态只能为0或1", null));
        }
        LambdaQueryWrapper<TicketUser> wrapper = new LambdaQueryWrapper<>();
        // 必须保留：所有查询限定在当前访问码对应的空间。
        wrapper.eq(TicketUser::getWorkspaceId, workspaceId);
        if (role != null && !role.isEmpty()) {
            wrapper.eq(TicketUser::getRole, role);
        }
        if (status != null) {
            wrapper.eq(TicketUser::getStatus, status);
        }
        wrapper.orderByAsc(TicketUser::getUserNo);
        return ResponseEntity.ok(Result.success(ticketUserService.list(wrapper)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Result<TicketUser>> getOneUser(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("userId") String userId) {
        LambdaQueryWrapper<TicketUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketUser::getWorkspaceId, workspaceId)
                .eq(TicketUser::getUserNo, userId);
        TicketUser user = ticketUserService.getOne(wrapper);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "用户不存在", null));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    @GetMapping("/activities")
    public ResponseEntity<Result<List<TicketActivity>>> getAllActivities(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam(value = "campus", required = false) String campus,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        // 校区不传或为空白时，不筛选。
        if (campus != null) {
            campus = campus.trim();
            if (!campus.isEmpty() && !"新吴校区".equals(campus) && !"藕塘校区".equals(campus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Result<>(400, "校区只能为新吴或藕塘", null));
            }
        }
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.length() > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Result<>(400, "关键词不能超过100个字符", null));
            }
        }
        if (status != null && status != 0 && status != 1 && status != 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "状态只能为0,1或2", null));
        }
        LambdaQueryWrapper<TicketActivity> wrapper = new LambdaQueryWrapper<>();
        // 必须保留：所有查询限定在当前访问码对应的空间。
        wrapper.eq(TicketActivity::getWorkspaceId, workspaceId);
        if (campus != null && !campus.isEmpty()) {
            wrapper.eq(TicketActivity::getCampus, campus);
        }
        if (status != null) {
            wrapper.eq(TicketActivity::getStatus, status);
        }
        if (keyword != null) {
            wrapper.like(TicketActivity::getActivityName, keyword);
        }
        wrapper.orderByDesc(TicketActivity::getId);
        return ResponseEntity.ok(Result.success(ticketActivityService.list(wrapper)));
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<Result<TicketActivity>> getOneActivity(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("activityId") Long activityId) {
        LambdaQueryWrapper<TicketActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketActivity::getWorkspaceId, workspaceId)
                .eq(TicketActivity::getId, activityId);
        TicketActivity activity = ticketActivityService.getOne(wrapper);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "活动不存在", null));
        }
        return ResponseEntity.ok(Result.success(activity));
    }

    @PostMapping("/activities")
    public ResponseEntity<Result<TicketActivity>> saveNewActivity(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestBody TicketActivityDTO ticketActivityDTO) {
        TicketActivity result = ticketActivityService.saveNewActivity(workspaceId, ticketActivityDTO);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Result<>(403, "操作人不存在、已停用或不是当前空间的管理员", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/activities/{activityId}/status")
    public ResponseEntity<Result<TicketActivity>> changeActivityStatus(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("activityId") Long activityId,
            @RequestBody TicketActivityStatusDTO dto) {
        TicketActivity result = ticketActivityService.changeActivityStatus(workspaceId, activityId, dto);
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/activities/{activityId}/records")
    public ResponseEntity<Result<TicketRecord>> bookTicket(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("activityId") Long activityId,
            @RequestParam("userId") Long userId) {
        TicketRecord result = ticketRecordService.bookTicket(workspaceId, activityId, userId);
        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/records/{recordId}/cancel")
    public ResponseEntity<Result<TicketRecord>> cancelTicket(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("recordId") Long recordId,
            @RequestParam("userId") Long userId) {
        TicketRecord result = ticketRecordService.cancelTicket(workspaceId, recordId, userId);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/users/{userId}/records")
    public ResponseEntity<Result<List<TicketRecord>>> getUserRecords(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("userId") Long userId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "activityId", required = false) Long activityId) {
        List<TicketRecord> result = ticketRecordService.getUserRecords(workspaceId, userId, status, activityId);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/activities/{activityId}/records")
    public ResponseEntity<Result<List<TicketRecord>>> getActivityRecords(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("activityId") Long activityId,
            @RequestParam("operatorId") Long operatorId,
            @RequestParam(value = "status", required = false) Integer status) {
        List<TicketRecord> result = ticketRecordService.getActivityRecords(workspaceId, activityId, operatorId, status);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/records/{recordId}")
    public ResponseEntity<Result<TicketRecord>> getRecordDetail(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("recordId") Long recordId,
            @RequestParam("operatorId") Long operatorId) {
        TicketRecord result = ticketRecordService.getRecordDetail(workspaceId, recordId, operatorId);
        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/records/{recordId}/verify")
    public ResponseEntity<Result<TicketRecord>> verifyTicket(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @PathVariable("recordId") Long recordId,
            @RequestParam("operatorId") Long operatorId) {
        TicketRecord result = ticketRecordService.verifyTicket(workspaceId, recordId, operatorId);
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/users/switch")
    public ResponseEntity<Result<TicketUser>> switchUser(
            @RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId,
            @RequestParam("userNo") String userNo) {
        TicketUser user = ticketUserService.switchUser(workspaceId, userNo);
        return ResponseEntity.ok(Result.success(user));
    }
}