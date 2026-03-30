package com.team.authservice.auth.security.jwt;

import com.team.authservice.core.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;

    // yml의 시크릿키 문자열을 서명용 키 객체로 변환하기
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성
    public String generateAccessToken(UUID userId, Role role) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.accessTokenValidity().toMillis()))
            .signWith(getSigningKey())
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.refreshTokenValidity().toMillis()))
            .signWith(getSigningKey())
            .compact();
    }
}
