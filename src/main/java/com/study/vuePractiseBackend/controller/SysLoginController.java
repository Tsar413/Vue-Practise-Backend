package com.study.vuePractiseBackend.controller;

import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.dto.SysLoginDTO;
import com.study.vuePractiseBackend.dto.SysLoginReturnDTO;
import com.study.vuePractiseBackend.service.SysLoginService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class SysLoginController {

    @Resource
    private SysLoginService service;

    @PostMapping("/login")
    public ResponseEntity<Result<SysLoginReturnDTO>> login(@RequestBody SysLoginDTO sysLoginDTO){
        SysLoginReturnDTO result = service.login(sysLoginDTO);
        Integer status = result.getStatus();

        if (Integer.valueOf(-2).equals(status)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "账号和密码不能为空", null));
        }

        if (Integer.valueOf(-3).equals(status)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "账号和密码不能超过50个字符", null));
        }

        if (Integer.valueOf(-4).equals(status)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Result<>(401, "账号或密码错误", null));
        }

        if (Integer.valueOf(-5).equals(status)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Result<>(403, "账号已停用", null));
        }

        if (Integer.valueOf(-6).equals(status)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Result<>(403, "所属班级不存在或已停用", null));
        }

        if (Integer.valueOf(-7).equals(status)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Result<>(403, "账号角色异常，无法登录", null));
        }

        if (!Integer.valueOf(1).equals(status)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "登录失败", null));
        }

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Result.success(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Result<Integer>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Result<>(401, "缺少有效的登录凭证", null));
        }
        String rawToken = authorization.substring(7).trim();
        Integer result = service.logout(rawToken);
        if (Integer.valueOf(-2).equals(result)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Result<>(401, "登录凭证格式不正确", null));
        }
        if (!Integer.valueOf(1).equals(result)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "退出登录失败", null));
        }
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Result.success(result));
    }
}
