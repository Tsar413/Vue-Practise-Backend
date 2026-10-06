package com.study.vuePractiseBackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "repair.files")
public class RepairFileProperties {
    // 相对启动工作目录；IDE请将Working directory设为项目根目录。
    private String root = "./repair-files";
}
