package com.team.order_service.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(new AuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth

                // 내부 API - 서비스 간 통신 (인증 불필요)
                .requestMatchers("/internal/**").permitAll()

                // 주문 생성
                .requestMatchers(HttpMethod.POST, "/api/v1/orders")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN", "HUB_DELIVERY_MANAGER", "COMPANY_MANAGER")

                // 주문 목록 조회
                .requestMatchers(HttpMethod.GET, "/api/v1/orders")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN", "HUB_DELIVERY_MANAGER", "COMPANY_MANAGER")

                // 주문 단건 조회
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/**")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN", "HUB_DELIVERY_MANAGER", "COMPANY_MANAGER")

                // 주문 수정
                .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/**")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                // 주문 취소/삭제
                .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                .anyRequest().authenticated()
            );
        return http.build();
    }
}
