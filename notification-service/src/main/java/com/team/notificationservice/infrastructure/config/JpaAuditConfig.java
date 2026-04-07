package com.team.notificationservice.infrastructure.config;

import com.team.common.Constants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            // 상황 A: HTTP 요청 컨텍스트가 없음 (비동기 이벤트 리스너, 스케줄러 등)
            if (attrs == null) {
                return Optional.of(Constants.SYSTEM_UUID);
                // 진짜 시스템 UUID 사용
            }
            // 상황 B: HTTP 요청은 왔는데 헤더가 문제가 있는 경우
            HttpServletRequest request = attrs.getRequest();
            String userId = request.getHeader("X-User-Id");

            if (userId == null || userId.isBlank()) {
                // 헤더가 없음 -> 에러 추적용 UUID 사용
                log.warn("X-User-Id 헤더가 누락되었습니다. URI: {}", request.getRequestURI());
                return Optional.of(Constants.UNKNOWN_USER_UUID);
            }

            try {
                return Optional.of(UUID.fromString(userId));
            } catch (IllegalArgumentException e) {
                log.warn("X-User-Id 헤더에 부정확한 UUID : {}",
                    userId.length() > 50 ? userId.substring(0, 50) + "..." : userId);
                // UUID 형식이 잘못됨 -> 에러 추적용 UUID 사용
                return Optional.of(Constants.UNKNOWN_USER_UUID);
            }
        };
    }
}
