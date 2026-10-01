package com.study.vuePractiseBackend.controller;

import com.study.vuePractiseBackend.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys-user")
public class SysUserController {

    @Resource
    private SysUserService service;



}
