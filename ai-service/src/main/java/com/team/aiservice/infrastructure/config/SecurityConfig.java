package com.team.aiservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. 내부 호출용: 시스템 간 통신 헤더 검증
                .requestMatchers("/internal/v1/**").access((authentication, context) -> {
                    String internalHeader = context.getRequest().getHeader("X-Internal-Request");
                    return new AuthorizationDecision("true".equals(internalHeader));
                })

                // 2. 외부 API용: Gateway가 넘겨준 유저 ID 텍스트 존재 여부 확인
                .requestMatchers("/api/v1/ais/**").access((authentication, context) -> {
                    String userId = context.getRequest().getHeader("X-User-Id");
                    // null-safe한 StringUtils.hasText 사용
                    return new AuthorizationDecision(StringUtils.hasText(userId));
                })

                // 3. Eureka 상태 점검용 Actuator 허용
                .requestMatchers("/actuator/**").permitAll()

                // 4. 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
