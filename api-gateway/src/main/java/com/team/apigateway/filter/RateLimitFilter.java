package com.team.apigateway.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimitScript;

    private static final String LOGIN_URI = "/auth/login";
    private static final long MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
        throws ServletException, IOException {

            // 로그인 경로 아니면 게이트웨이로 넘김
            if(!LOGIN_URI.equals(request.getRequestURI())){
                filterChain.doFilter(request, response);
                return;
            }

            String ip = request.getRemoteAddr();
            String key = "rate_limit:login:" + ip;

            Long count;
            try{
                count = redisTemplate.execute(
                    rateLimitScript, // 실행할 Lua 스크립트
                    List.of(key), // KEYS 배열 : Lua 스크립트 안의 KEYS[1]에 매핑됨
                    String.valueOf(WINDOW_SECONDS) // Lua 스크립트 안의 ARGV[1]에 매핑
                );
            }catch (DataAccessException e){
                filterChain.doFilter(request, response);
                return;
            }

            // 서비스 가용이 우선인 경우 : count == null -> Redis 장애 시 일단 통과
            // 보안이 우선일 때 : count == null -> Redis 장애 시에도 차단
            if(count != null && count > MAX_REQUESTS){
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\": \"Too many requests\"}");
                return;
            }

            filterChain.doFilter(request, response);
    }
}
