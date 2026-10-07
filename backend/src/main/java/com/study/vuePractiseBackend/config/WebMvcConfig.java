package com.study.vuePractiseBackend.config;

import com.study.vuePractiseBackend.interceptor.ApiAccessInterceptor;
import com.study.vuePractiseBackend.interceptor.SysLoginInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private SysLoginInterceptor sysLoginInterceptor;

    @Resource
    private ApiAccessInterceptor apiAccessInterceptor;

    @Value("${app.cors.allowed-origins:"
            + "http://localhost:5173,"
            + "http://127.0.0.1:5173,"
            + "http://localhost:4173,"
            + "http://127.0.0.1:4173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
                .exposedHeaders(
                        "Content-Disposition",
                        "Content-Type"
                )
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessInterceptor)
                .addPathPatterns("/api/practice/**")
                .order(0);

        registry.addInterceptor(sysLoginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/error",
                        "/hello",
                        "/api/practice/**"
                )
                .order(1);
    }
}