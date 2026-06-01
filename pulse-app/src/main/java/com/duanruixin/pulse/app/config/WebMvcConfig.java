package com.duanruixin.pulse.app.config;

import com.duanruixin.pulse.app.interceptor.ApiAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiAuthInterceptor apiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAuthInterceptor)
                // 只对 /api/external/** 路径生效,内部管理接口 /api/v1/** 不需要 API Key
                .addPathPatterns("/api/external/**");
    }
}