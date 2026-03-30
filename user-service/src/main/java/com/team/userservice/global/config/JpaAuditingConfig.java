package com.team.userservice.global.config;

import java.util.Optional;
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
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            String userId = Optional.ofNullable(
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes()
                )
                .map(attr -> attr.getRequest().getHeader("X-User-Id"))
                .orElse("anonymous");
            return Optional.of(userId);
        };
    }

}
