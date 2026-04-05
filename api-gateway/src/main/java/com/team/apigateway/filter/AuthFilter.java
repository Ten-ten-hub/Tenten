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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

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

        UUID userId;
        String role;
        String hubId = null;
        String companyId = null;

        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            userId = UUID.fromString(claims.getSubject());
            role = claims.get("role", String.class);
            hubId = claims.get("hubId", String.class);
            companyId = claims.get("companyId", String.class);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (role == null || role.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final UUID finalUserId = userId;
        final String finalRole = role;
        final String finalHubId = hubId;
        final String finalCompanyId = companyId;

        HttpServletRequest mutatedRequest = new HttpServletRequestWrapper(request) {

            @Override
            public String getHeader(String name) {
                return switch (name) {
                    case "X-User-Id" -> finalUserId.toString();
                    case "X-User-Role" -> finalRole;
                    case "X-Hub-Id" -> finalHubId;
                    case "X-Company-Id" -> finalCompanyId;
                    default -> super.getHeader(name);
                };
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Id".equals(name)) {
                    return Collections.enumeration(List.of(finalUserId.toString()));
                }
                if ("X-User-Role".equals(name)) {
                    return Collections.enumeration(List.of(finalRole));
                }
                if ("X-Hub-Id".equals(name) && finalHubId != null) {
                    return Collections.enumeration(List.of(finalHubId));
                }
                if ("X-Company-Id".equals(name) && finalCompanyId != null) {
                    return Collections.enumeration(List.of(finalCompanyId));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add("X-User-Id");
                names.add("X-User-Role");
                if (finalHubId != null) {
                    names.add("X-Hub-Id");
                }
                if (finalCompanyId != null) {
                    names.add("X-Company-Id");
                }
                return Collections.enumeration(names);
            }
        };

        filterChain.doFilter(mutatedRequest, response);
    }
}
