package com.erp.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Resource
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 所有接口
                .allowCredentials(true) // 允许携带Cookie
                .allowedOriginPatterns("*") // 允许所有来源（生产环境建议指定具体域名）
                .allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE"}) // 允许的HTTP方法
                .allowedHeaders("*") // 允许所有请求头
                .exposedHeaders("*");

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/error",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/v2/**",
                        "/webjars/**",
                        "/favicon.ico"
                );
    }
}
