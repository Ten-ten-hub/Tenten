package com.team.notificationservice.infrastructure.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class FeignHeaderConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> template.header("X-Internal-Request", "true");
    }
}
