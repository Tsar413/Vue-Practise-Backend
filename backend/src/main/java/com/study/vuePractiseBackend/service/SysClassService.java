package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.SysClassDTO;
import com.study.vuePractiseBackend.entity.SysClass;

public interface SysClassService extends IService<SysClass> {
    Integer addNewClass(SysClassDTO sysClassDTO);

    Integer changeClass(SysClassDTO sysClassDTO);

    Integer deleteById(String id);

    Integer changeClassStatus(String id, int status);
}
