package com.team.product_service.global.config;

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

                // 상품 내부 API - 서비스 간 통신 (인증 불필요)
                .requestMatchers("/internal/**").permitAll()

                // 상품 생성
                .requestMatchers(HttpMethod.POST, "/api/v1/products")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN", "COMPANY_MANAGER")

                // 상품 수정
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN", "COMPANY_MANAGER")

                // 상품 삭제
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                // 재고 수정
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/*/stock")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN", "COMPANY_MANAGER")

                // 상품 조회 - 전체 허용
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                // 재고 이력 조회 - 전체 허용
                .requestMatchers(HttpMethod.GET, "/api/v1/stocks/**").permitAll()

                .anyRequest().authenticated()
            );
        return http.build();
    }
}
