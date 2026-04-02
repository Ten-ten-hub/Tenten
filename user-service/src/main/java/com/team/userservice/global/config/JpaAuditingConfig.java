package com.team.userservice.global.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableJpaAuditing
@Profile("!test")
public class JpaAuditingConfig {

    private static final UUID ANONYMOUS_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            String userId = Optional.ofNullable(
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes()
                )
                .map(attr -> attr.getRequest().getHeader("X-User-Id"))
                .orElse(null);

            if (userId == null || userId.isBlank()) {
                return Optional.of(ANONYMOUS_USER_ID);
            }

            try {
                return Optional.of(UUID.fromString(userId));
            } catch (IllegalArgumentException e) {
                return Optional.of(ANONYMOUS_USER_ID);
            }
        };
    }
}
