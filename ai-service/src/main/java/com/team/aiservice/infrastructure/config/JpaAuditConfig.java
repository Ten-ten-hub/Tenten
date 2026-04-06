package com.team.aiservice.infrastructure.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class JpaAuditConfig {
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String userId = attributes.getRequest().getHeader("X-User-Id");
                if (userId != null && !userId.isEmpty()) {
                    return Optional.of(UUID.fromString(userId));
                }
            }
            return Optional.empty();
        };
    }
}
