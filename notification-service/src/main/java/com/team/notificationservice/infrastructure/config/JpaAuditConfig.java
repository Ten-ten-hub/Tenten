package com.team.notificationservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import java.util.Optional;
import java.util.UUID;

@Configuration
public class JpaAuditConfig {
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        // 인증 정보가 없을 때 null 대신 시스템 ID(0000...)를 강제로 넣음 - BaseEntity 제약 때문에 작성
        return () -> Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }
}
