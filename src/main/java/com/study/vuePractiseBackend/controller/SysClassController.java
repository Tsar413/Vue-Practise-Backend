package com.study.vuePractiseBackend.controller;

import com.study.vuePractiseBackend.common.Result;
import com.study.vuePractiseBackend.dto.SysClassDTO;
import com.study.vuePractiseBackend.entity.SysClass;
import com.study.vuePractiseBackend.service.SysClassService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys-class")
public class SysClassController {
    @Resource
    private SysClassService service;

    @GetMapping("/all")
    public ResponseEntity<Result<List<SysClass>>> getAllClasses(){
        List<SysClass> list = service.list();
        return ResponseEntity.ok(Result.success(list));
    }

    @GetMapping("/one/{id}")
    public ResponseEntity<Result<SysClass>> getOneClass(@PathVariable("id") String id){
        SysClass sysClass = service.getById(id);
        if(sysClass == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Result<SysClass>(404, "班级不存在", null));
        }
        return ResponseEntity.ok(Result.success(sysClass));
    }

    @PostMapping("/one")
    public ResponseEntity<Result<Integer>> addNewClass(@RequestBody SysClassDTO sysClassDTO){
        Integer result = service.addNewClass(sysClassDTO);
        if(result == -2){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<Integer>
                    (400, "班级id或名字为空", null));
        }
        if(result == -3){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<Integer>
                    (400, "班级id或名字过长", null));
        }
        if(result == -4){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<Integer>
                    (400, "班级已存在", null));
        }
        if (result != 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "添加班级失败", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/one")
    public ResponseEntity<Result<Integer>> changeClass(@RequestBody SysClassDTO sysClassDTO){
        Integer result = service.changeClass(sysClassDTO);
        if(result == -2){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<Integer>
                    (400, "班级id或名字为空", null));
        }
        if(result == -3){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<Integer>
                    (400, "班级id或名字过长", null));
        }
        if(result == -4){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Result<Integer>
                    (404, "班级不存在", null));
        }
        if (result != 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "修改班级失败", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/one/{id}/status")
    public ResponseEntity<Result<Integer>> changeClassStatus(@PathVariable("id") String id, @RequestParam("status") Integer status){
        Integer result = service.changeClassStatus(id, status);
        if(result == -2){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<Integer>
                    (400, "班级id为空", null));
        }
        if(result == -3){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<Integer>
                    (400, "班级id过长", null));
        }
        if(result == -4){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Result<Integer>
                    (404, "班级不存在", null));
        }
        if (result == -5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Result<>(400, "状态只能为0或1", null));
        }
        if (result != 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "修改班级失败", null));
        }
        // TODO SysUser结果
        return ResponseEntity.ok(Result.success(result));
    }

    @DeleteMapping("/one/{id}")
    public ResponseEntity<Result<Integer>> deleteClass(@PathVariable("id") String id){
        Integer result = service.deleteById(id);
        if (result != 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Result<>(500, "删除班级失败", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }
}
