package com.team.apigateway.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RateLimitScript {

    @Bean
    public RedisScript<Long> rateLimitScript() {
        String script =
            "local count = redis.call('INCR', KEYS[1])" + // 키가 있으면 : KEY[1]의 값을 1증가시키고 count에 저장 , 키가 없으면 : 0에서 시작해서 1로 증가
                "if count == 1 then" + // count가 1이면 이 키는 처음 생성된것
                "redis.call('EXPIRE',KEYS[1], ARGV[1])" + // 키에 만료 시간 설정 (ARGV[1]초 후에 자동 삭제)
                "end" +
                "return count"; // 현재 카운트 반환
        return RedisScript.of(script, Long.class);
    }
}
