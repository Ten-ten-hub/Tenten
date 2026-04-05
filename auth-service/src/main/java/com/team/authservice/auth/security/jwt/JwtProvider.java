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

/*
* JWT 구성 : 헤더 + 페이로드 + 서명
* 페이로드 : 실제 데이터가 담긴 부분. 여러 Claim들의 집합이며, Base64로 인코딩되어있다
* 서브젝트 : 페이로드 안의 클레임 중 하나. 키 이름 = sub // 이 토큰이 누구 것인지를 나타냄 -> UserId가 저장되는 곳
* */

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
    public String generateAccessToken(UUID userId, Role role, UUID hubId, UUID companyId) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name())
            .claim("hubId", hubId != null ? hubId.toString() : null)
            .claim("companyId", companyId != null ? companyId.toString() : null)
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

    // rt로부터 유저아이디 추출
    public UUID extractUserInfo(String refreshToken){
        String userId = Jwts.parser()//JWT parser 빌더 생성
            .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret())))//서명 검증에 사용하는 키 설정(Base64로 인코딩된 시크릿 문자열을 디코딩해서 HMAC 키 객체로 변환)
            .build() //설정이 완료된 parser 객체 생성
            .parseSignedClaims(refreshToken)//실제 파싱과 서명 검증, 토큰 변조나 만료시 여기서 예외 터짐
            .getPayload() // 검증된 토큰에서 페이로드(Claims)를 꺼낸다
            .getSubject();// 페이로드(Claims)에서 subject를 꺼낸다. generateRefreshToken에서 .subject(userId.toString())으로 넣었던 값

        return UUID.fromString(userId);
    }

}
