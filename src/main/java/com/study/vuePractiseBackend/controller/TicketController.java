package com.study.vuePractiseBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.entity.TicketUser;
import com.study.vuePractiseBackend.interceptor.ApiAccessInterceptor;
import com.study.vuePractiseBackend.mapper.TickerUserMapper;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/practice/{accessCode}/ticket")
public class TicketController {

    @Resource
    private TickerUserMapper tickerUserMapper;

    @GetMapping("/all")
    public ResponseEntity<Result<List<TicketUser>>> getAllUsers(@RequestAttribute(ApiAccessInterceptor.WORKSPACE_ID) Long workspaceId) {
        QueryWrapper<TicketUser> wrapper = new QueryWrapper<>();
        wrapper.eq("workspace_id", workspaceId)
                .orderByAsc("user_no");
        return ResponseEntity.ok(Result.success(tickerUserMapper.selectList(wrapper)));
    }
}