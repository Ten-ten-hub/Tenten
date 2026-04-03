package com.team.authservice.auth.infrastructure;

import com.team.authservice.auth.security.jwt.JwtProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisTokenRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    public void save(UUID userId, String refreshToken) {
        redisTemplate.opsForValue().set("refresh_token:" + userId, refreshToken, jwtProperties.refreshTokenValidity());
    }

    public void delete(UUID userId) {
        redisTemplate.delete("refresh_token:" + userId);
    }

    public String findByUserId(UUID userId) {
        //값을 가져와서 비교하기위해
        return redisTemplate.opsForValue().get("refresh_token:" + userId);

    }
}
