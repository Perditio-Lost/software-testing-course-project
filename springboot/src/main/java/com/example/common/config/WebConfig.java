package com.example.common.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SuppressWarnings("null")
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JWTInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/")
                .excludePathPatterns("/login", "/register", "/files/**", "/user/sendCode", "/user/emailRegister",
                        "/user/sendResetCode", "/user/resetPassword", "/test/**", "/livekit/**");
    }

}
