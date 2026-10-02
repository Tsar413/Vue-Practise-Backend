package com.study.vuePractiseBackend.controller;

import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.dto.SysUserDTO;
import com.study.vuePractiseBackend.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys-user")
public class SysUserController {

    @Resource
    private SysUserService service;

    @PostMapping("/one")
    public ResponseEntity<Result<Integer>> createOneSysUser(@RequestBody SysUserDTO sysUserDTO){
        Integer result = service.createOneSysUser(sysUserDTO);

        if (result == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "用户编号、姓名和角色不能为空", null));
        }

        if (result == -3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(
                            400,
                            "用户编号、姓名不能为空，角色只能为TEACHER或STUDENT",
                            null
                    ));
        }

        if (result == -4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "学生必须填写班级编号", null));
        }

        if (result == -5) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "班级不存在", null));
        }

        if (result == -6) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Result<>(409, "用户编号已存在", null));
        }

        if (result == -7) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(
                            400,
                            "用户编号、用户名、姓名和班级编号均不能超过50个字符",
                            null
                    ));
        }

        if (result == -8) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Result<>(409, "班级已停用，无法添加学生", null));
        }

        if (result != 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "创建用户失败", null));
        }

        return ResponseEntity.ok(Result.success(result));
    }

}
