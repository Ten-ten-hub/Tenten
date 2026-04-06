package com.team.deliveryservice.support;

import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@TestConfiguration
@EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
public class TestJpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> testAuditorAware() {
        return () -> Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }
}
