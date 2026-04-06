package com.team.aiservice.application.service;

import com.team.aiservice.infrastructure.client.HubClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubRouteCacheService {

    private final HubClient hubClient;

    /**
     * Redis에서 경로 데이터를 먼저 찾고, 없으면 API 호출 후 캐싱 (RAG 최적화)
     */
    @Cacheable(cacheNames = "hubRoutes", key = "#originId.toString() + ':' + #destId.toString()", unless = "#result == null")
    public HubClient.HubRouteResponse getCachedRoute(UUID originId, UUID destId) {
        log.info("[RAG Cache Miss] 허브 서비스 호출: {} -> {}", originId, destId);

        try {
            return hubClient.getRoute(originId, destId);
        } catch (feign.FeignException.NotFound e) {
            // 404 발생 시 에러를 던지지 않고 null을 반환하여 서비스 로직에서 예외 처리하게 함
            log.warn("[HUB NOT FOUND] 해당 경로 정보가 없습니다: {} -> {}", originId, destId);
            return null;
        } catch (Exception e) {
            log.error("[HUB ERROR] 허브 서비스 호출 중 알 수 없는 오류 발생", e);
            throw e;
        }
    }
}
