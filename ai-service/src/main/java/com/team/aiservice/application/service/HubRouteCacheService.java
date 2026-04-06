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
    @Cacheable(cacheNames = "hubRoutes", key = "#originId.toString() + ':' + #destId.toString()")
    public HubClient.HubRouteResponse getCachedRoute(UUID originId, UUID destId) {
        log.info("[RAG Cache Miss] 허브 서비스 호출 (Internal Header 포함): {} -> {}", originId, destId);

        return hubClient.getRoute(originId, destId);
    }
}
