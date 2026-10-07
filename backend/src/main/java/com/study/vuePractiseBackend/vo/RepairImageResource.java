package com.study.vuePractiseBackend.vo;

import org.springframework.core.io.Resource;

// Service向Controller返回文件内容，不作为JSON返回。
public record RepairImageResource(Resource resource, String contentType, String originalName) {
}
