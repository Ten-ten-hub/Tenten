package com.team.notificationservice.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class JpaAuditConfig {
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null && !userId.isBlank()) {
                        return Optional.of(UUID.fromString(userId));
                    }
                }
            } catch (Exception ignored) {
            }
            // 인증 정보가 없을 때 시스템 ID 반환
            return Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        };
    }
}
