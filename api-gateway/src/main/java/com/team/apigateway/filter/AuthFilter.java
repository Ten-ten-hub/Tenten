package com.team.apigateway.filter;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
public class AuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        List<String> publicRoutes = List.of("/auth/login", "/users/signup", "/auth/refresh");
        if (publicRoutes.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = authHeader.substring(7);
        UUID userId = null;
        String role = null;
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
            userId = UUID.fromString(claims.getSubject());
            role = claims.get("role", String.class);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (role == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final UUID finalUserId = userId;
        final String finalRole = role;

        //HttpServletRequestWrapper : 기존 요청을 감싸는 래퍼, getHeader를 오버라이드해서 헤더 읽을 때 동작을 바꿈
        //ex) downstream 서비스가 request.getHeader("X-User-Id")를 호출하면 아래의 오버라이드된 메서드가 실행됨
        HttpServletRequest mutatedRequest = new HttpServletRequestWrapper(
            request) { // -> List로 변환해서 요소 추가 후 다시 Enumeration 반환
            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add("X-User-Id");
                names.add("X-User-Role");
                return Collections.enumeration(names);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Id".equals(name)) {
                    return Collections.enumeration(List.of(finalUserId.toString()));
                }
                if ("X-User-Role".equals(name)) {
                    return Collections.enumeration(List.of(finalRole));
                }

                return super.getHeaders(name);
            }

            @Override
            public String getHeader(String name) { //헤더를 읽는 동작을 가로채서 JWT에서 꺼낸 값을 대신 반환
                if ("X-User-Id".equals(name)) {
                    return finalUserId.toString();
                }
                if ("X-User-Role".equals(name)) {
                    return finalRole;
                }
                return super.getHeader(name);
            }
        };

        filterChain.doFilter(mutatedRequest, response);
    }
}

