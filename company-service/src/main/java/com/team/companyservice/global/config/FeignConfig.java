package com.team.companyservice.global.config;

import com.team.companyservice.global.common.CurrentUser;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) return;

            var request = attributes.getRequest();

            template.header("X-User-Id", request.getHeader("X-User-Id"));
            template.header("X-User-Role", request.getHeader("X-User-Role"));
            template.header("X-Hub-Id", request.getHeader("X-Hub-Id"));
            template.header("X-Company-Id", request.getHeader("X-Company-Id"));
        };
    }
}
