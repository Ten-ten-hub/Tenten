package com.team.hubservice.global.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    private static final UUID SYSTEM_ACTOR = UUID.fromString("7f938f32-8e12-4f32-8b2b-6f4e3c2d1b0a");

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.of(SYSTEM_ACTOR);
    }
}
