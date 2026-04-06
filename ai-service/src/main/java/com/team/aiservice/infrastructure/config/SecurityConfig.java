package com.team.aiservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. 내부 호출용: 시큐리티 인증은 건너뛰고(permitAll), 헤더 로직(access)으로 통제
                .requestMatchers("/internal/v1/ais/**").access((authentication, context) -> {
                    String internalHeader = context.getRequest().getHeader("X-Internal-Request");
                    return new AuthorizationDecision("true".equals(internalHeader));
                })

                // 2. 외부 API용: Gateway가 넘겨준 X-User-Id 헤더가 있으면 통과
                .requestMatchers("/api/v1/ais/**").access((authentication, context) -> {
                    String userId = context.getRequest().getHeader("X-User-Id");
                    return new AuthorizationDecision(userId != null && !userId.isBlank());
                })

                // 3. 그 외 모든 요청은 차단
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
