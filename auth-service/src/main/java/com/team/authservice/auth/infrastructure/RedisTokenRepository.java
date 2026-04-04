package com.team.authservice.auth.infrastructure;

import com.team.authservice.auth.security.jwt.JwtProperties;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    public boolean compareAndReplace(UUID userId, String expectedToken, String newToken) {
        String key = "refresh_token:" + userId;  // 1. Redis 키 구성

        // 2. Lua 스크립트 정의
        String script =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +  // 현재 값 == 기대값?
                "  redis.call('setex', KEYS[1], ARGV[3], ARGV[2]) " + // 같으면 새 값으로 교체 + TTL 설정
                "  return 1 " +                                         // 성공 반환
                "else " +
                "  return 0 " +                                         // 실패 반환
                "end";

        // ③ Spring Data Redis로 Lua 스크립트 실행
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            List.of(key),           // KEYS[1] = "refresh_token:{userId}"
            expectedToken,          // ARGV[1] = 클라이언트가 보낸 기존 RT
            newToken,               // ARGV[2] = 새로 발급할 RT
            String.valueOf(jwtProperties.refreshTokenValidity().getSeconds()) // ARGV[3] = TTL(초)
        );

        return result != null && result == 1; // 1이면 교체 성공, 0이면 실패
    }
}
