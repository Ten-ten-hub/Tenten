package com.team.order_service.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            try {
                ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes == null) return Optional.empty();

                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader("X-User-Id");

                if (userId == null || userId.isBlank()) return Optional.empty();

                return Optional.of(UUID.fromString(userId));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
}
