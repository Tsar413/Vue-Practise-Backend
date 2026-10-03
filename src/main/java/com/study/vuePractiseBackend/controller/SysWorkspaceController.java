package com.study.vuePractiseBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.entity.SysUser;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import com.study.vuePractiseBackend.service.SysWorkspaceService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys-workspace")
public class SysWorkspaceController {
    @Resource
    private SysWorkspaceService service;

    @GetMapping("/classes/{id}")
    public ResponseEntity<Result<List<SysWorkspace>>> getWorkspacesClassId(@PathVariable("id") String id){
        return ResponseEntity.ok(Result.success(service.getWorkspacesClassId(id)));
    }

    @GetMapping("/one/{id}")
    public ResponseEntity<Result<SysWorkspace>> getOneWorkspace(@PathVariable("id") String id) {
        QueryWrapper<SysWorkspace> wrapper = new QueryWrapper<>();
        wrapper.eq("student_id", id.trim());
        SysWorkspace workspace = service.getOne(wrapper);
        if (workspace == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "工作空间不存在", null));
        }
        return ResponseEntity.ok(Result.success(workspace));
    }

    @PutMapping("/one/{id}/status")
    public ResponseEntity<Result<Integer>> changeWorkspaceStatus(@PathVariable("id") String studentId, @RequestParam("status") Integer status) {
        Integer result = service.changeWorkspaceStatus(studentId, status);
        if (result == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "学号不能为空", null));
        }
        if (result == -3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "学号不能超过50个字符", null));
        }
        if (result == -4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "工作空间不存在", null));
        }
        if (result == -5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "状态只能为0或1", null));
        }
        if (result == -6) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Result<>(404, "工作空间所属用户不存在", null));
        }
        if (result == -7) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Result<>(409, "仅学生账号可以启用工作空间", null));
        }
        if (result == -8) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Result<>(409, "用户已停用，无法启用工作空间", null));
        }
        if (result == -10) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Result<>(409, "学生未关联有效班级，无法启用工作空间", null));
        }
        if (result == -11) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Result<>(409, "所属班级已停用，无法启用工作空间", null));
        }
        if (!Integer.valueOf(1).equals(result)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "修改工作空间状态失败", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/one/{id}/reset")
    public ResponseEntity<Result<Integer>> resetWorkspace(@PathVariable("id") String studentId, @RequestParam("project") String project) {

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(new Result<>(501, "未完待续", null));
    }
}
