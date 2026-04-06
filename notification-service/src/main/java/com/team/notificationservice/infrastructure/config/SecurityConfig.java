package com.team.notificationservice.infrastructure.config;

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
                // 1. 내부 호출
                .requestMatchers("/internal/v1/notifications/**").access((authentication, context) -> {
                    String internalHeader = context.getRequest().getHeader("X-Internal-Request");
                    return new AuthorizationDecision("true".equals(internalHeader));
                })
                // 2. 외부 API 호출 (X-User-Id 헤더가 있으면 인증된 유저로 간주)
                .requestMatchers("/api/v1/notifications/**").access((authentication, context) -> {
                    String userId = context.getRequest().getHeader("X-User-Id");
                    // Gateway에서 인증 필터를 거쳐 X-User-Id가 넘어왔다면 허용
                    return new AuthorizationDecision(userId != null && !userId.isBlank());
                })
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
