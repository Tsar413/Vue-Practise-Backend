package com.study.vuePractiseBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.dto.StudentImportResultDTO;
import com.study.vuePractiseBackend.dto.SysUserDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<StudentImportResultDTO>> importStudents(@RequestParam("file") MultipartFile file) {
        StudentImportResultDTO report = service.importStudents(file);
        String message = "处理完成：成功"
                + report.getSuccessCount()
                + "人，跳过"
                + report.getSkippedCount()
                + "人，失败"
                + report.getFailedCount()
                + "人";
        return ResponseEntity.ok(new Result<>(200, message, report));
    }

    @GetMapping("/all")
    public ResponseEntity<Result<List<SysUser>>> getAllUsers(){
        return ResponseEntity.ok(Result.success(service.list()));
    }

    @GetMapping("/one/{id}")
    public ResponseEntity<Result<SysUser>> getOneUser(@PathVariable("id") String id){
        SysUser sysUser = service.getById(id);
        if(sysUser == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Result<SysUser>(404, "老师/学生不存在", null));
        }
        return ResponseEntity.ok(Result.success(sysUser));
    }

    @GetMapping("/classes/{id}")
    public ResponseEntity<Result<List<SysUser>>> getClassesUsers(@PathVariable("id") String id){
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", id.trim());
        wrapper.eq("role", "STUDENT");
        wrapper.orderByAsc("id");
        return ResponseEntity.ok(Result.success(service.list(wrapper)));
    }

    @PutMapping("/one")
    public ResponseEntity<Result<Integer>> changeUser(@RequestBody SysUserDTO sysUserDTO){
        Integer result = service.changeUser(sysUserDTO);
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "用户不存在", null));
        }

        if (result == -5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "学生班级编号未填写或班级不存在", null));
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
                    .body(new Result<>(409, "目标班级已停用，无法修改学生信息", null));
        }

        if (!Integer.valueOf(1).equals(result)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "修改用户失败", null));
        }

        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/one/{id}/status")
    public ResponseEntity<Result<Integer>> changeUserStatus(@PathVariable("id") String id, @RequestParam("status") Integer status){
        Integer result = service.changeUserStatus(id, status);
        if (result == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "用户编号不能为空", null));
        }

        if (result == -3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "用户编号不能超过50个字符", null));
        }

        if (result == -4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "用户不存在", null));
        }

        if (result == -5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "状态只能为0或1", null));
        }

        if (!Integer.valueOf(1).equals(result)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "修改用户状态失败", null));
        }

        return ResponseEntity.ok(Result.success(result));
    }

    @DeleteMapping("/one/{id}")
    public ResponseEntity<Result<Integer>> deleteUser(@PathVariable("id") String id) {
        Integer result = service.deleteById(id);
        if (!Integer.valueOf(1).equals(result)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "删除用户失败", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }


}
