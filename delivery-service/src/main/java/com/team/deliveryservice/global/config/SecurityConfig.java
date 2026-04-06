package com.team.deliveryservice.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationFilter authenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"code\":\"COMMON_UNAUTHORIZED\",\"message\":\"인증 정보가 없거나 만료되었습니다. 다시 로그인해주세요.\",\"details\":null}"
                    );
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"code\":\"COMMON_ACCESS_DENIED\",\"message\":\"해당 기능에 대한 접근 권한이 없습니다.\",\"details\":null}"
                    );
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/internal/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/v1/deliveries")
                .hasAnyRole(
                    "MASTER_ADMIN",
                    "HUB_ADMIN",
                    "HUB_DELIVERY_MANAGER",
                    "COM_DELIVERY_MANAGER",
                    "COMPANY_MANAGER"
                )

                .requestMatchers(HttpMethod.GET, "/api/v1/deliveries/{deliveryId}")
                .hasAnyRole(
                    "MASTER_ADMIN",
                    "HUB_ADMIN",
                    "HUB_DELIVERY_MANAGER",
                    "COM_DELIVERY_MANAGER",
                    "COMPANY_MANAGER"
                )

                .requestMatchers(HttpMethod.PUT, "/api/v1/deliveries/{deliveryId}")
                .hasAnyRole(
                    "MASTER_ADMIN",
                    "HUB_ADMIN",
                    "HUB_DELIVERY_MANAGER",
                    "COM_DELIVERY_MANAGER"
                )

                .requestMatchers(HttpMethod.PATCH, "/api/v1/deliveries/{deliveryId}/assign-company-manager")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                .requestMatchers(HttpMethod.PATCH, "/api/v1/deliveries/{deliveryId}/assign-delivery-manager")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/v1/delivery-managers")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/v1/delivery-managers")
                .hasAnyRole(
                    "MASTER_ADMIN",
                    "HUB_ADMIN",
                    "HUB_DELIVERY_MANAGER",
                    "COM_DELIVERY_MANAGER"
                )

                .requestMatchers(HttpMethod.GET, "/api/v1/delivery-managers/{deliveryManagerId}")
                .hasAnyRole(
                    "MASTER_ADMIN",
                    "HUB_ADMIN",
                    "HUB_DELIVERY_MANAGER",
                    "COM_DELIVERY_MANAGER"
                )

                .requestMatchers(HttpMethod.PUT, "/api/v1/delivery-managers/{deliveryManagerId}")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                .requestMatchers(HttpMethod.DELETE, "/api/v1/delivery-managers/{deliveryManagerId}")
                .hasAnyRole("MASTER_ADMIN", "HUB_ADMIN")

                .anyRequest().authenticated()
            );

        return http.build();
    }
}
