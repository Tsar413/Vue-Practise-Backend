package com.study.vuePractiseBackend.config;

import com.study.vuePractiseBackend.interceptor.SysLoginInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private SysLoginInterceptor sysLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sysLoginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/error",
                        "/hello"
                );
    }
}