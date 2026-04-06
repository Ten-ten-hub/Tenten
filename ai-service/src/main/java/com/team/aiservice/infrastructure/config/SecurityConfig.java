package com.team.aiservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // 내부 호출 경로는 헤더 검증을 통해 시스템 호출만 허용
                .requestMatchers("/internal/v1/ais/**").access((authentication, context) -> {
                    String internalHeader = context.getRequest().getHeader("X-Internal-Request");
                    return new AuthorizationDecision("true".equals(internalHeader));
                })
                // 일반 API(조회 등)는 일단 다 열어두거나 권한 설정
                .requestMatchers("/api/v1/ais/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
