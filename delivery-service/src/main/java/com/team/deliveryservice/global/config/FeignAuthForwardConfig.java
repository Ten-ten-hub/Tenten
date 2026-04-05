package com.team.deliveryservice.global.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthForwardConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            var request = attributes.getRequest();

            copyHeaderIfPresent(request.getHeader("X-User-Id"), "X-User-Id", template);
            copyHeaderIfPresent(request.getHeader("X-User-Role"), "X-User-Role", template);
            copyHeaderIfPresent(request.getHeader("X-Hub-Id"), "X-Hub-Id", template);
            copyHeaderIfPresent(request.getHeader("X-Company-Id"), "X-Company-Id", template);
            copyHeaderIfPresent(request.getHeader("X-Internal-Request"), "X-Internal-Request", template);
        };
    }

    private void copyHeaderIfPresent(String value, String headerName, RequestTemplate template) {
        if (value != null && !value.isBlank()) {
            template.header(headerName, value);
        }
    }
}
