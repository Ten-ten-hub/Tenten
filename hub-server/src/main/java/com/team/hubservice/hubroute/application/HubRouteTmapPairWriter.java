package com.team.hubservice.hubroute.application;

import com.team.hubservice.global.config.CacheConfig;
import com.team.hubservice.hubroute.domain.HubRoute;
import com.team.hubservice.hubroute.domain.HubRouteRepository;
import com.team.hubservice.hubroute.infrastructure.tmap.TmapRouteMetrics;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HubRouteTmapPairWriter {

    private final HubRouteRepository hubRouteRepository;
    private final CacheManager cacheManager;

    public HubRouteTmapPairWriter(HubRouteRepository hubRouteRepository, CacheManager cacheManager) {
        this.hubRouteRepository = hubRouteRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public TmapHubRouteSyncResult upsert(UUID departureHubId, UUID arrivalHubId, TmapRouteMetrics metrics) {
        UUID affectedRouteId;
        TmapHubRouteSyncResult result;

        var existingOpt = hubRouteRepository.findByDepartureHubIdAndArrivalHubId(departureHubId, arrivalHubId);
        if (existingOpt.isPresent()) {
            HubRoute route = existingOpt.get();
            route.update(metrics.durationMinutes(), metrics.distanceKm());
            affectedRouteId = route.getId();
            result = TmapHubRouteSyncResult.updated(departureHubId, arrivalHubId, metrics);
        } else {
            CreateRouteOutcome outcome = createRoute(departureHubId, arrivalHubId, metrics);
            affectedRouteId = outcome.route().getId();
            result = outcome.syncResult();
        }

        evictRouteCache(affectedRouteId);
        evictOptimalRouteCacheAll();
        return result;
    }

    private CreateRouteOutcome createRoute(UUID departureHubId, UUID arrivalHubId, TmapRouteMetrics metrics) {
        HubRoute route = HubRoute.create(
            departureHubId,
            arrivalHubId,
            metrics.durationMinutes(),
            metrics.distanceKm()
        );
        try {
            HubRoute saved = hubRouteRepository.save(route);
            return new CreateRouteOutcome(
                saved,
                TmapHubRouteSyncResult.created(departureHubId, arrivalHubId, metrics)
            );
        } catch (DataIntegrityViolationException e) {
            HubRoute existing = hubRouteRepository.findByDepartureHubIdAndArrivalHubId(departureHubId, arrivalHubId)
                .orElseThrow(() -> e);
            existing.update(metrics.durationMinutes(), metrics.distanceKm());
            return new CreateRouteOutcome(
                existing,
                TmapHubRouteSyncResult.updated(departureHubId, arrivalHubId, metrics)
            );
        }
    }

    private record CreateRouteOutcome(HubRoute route, TmapHubRouteSyncResult syncResult) {}

    private void evictRouteCache(UUID routeId) {
        if (routeId == null) {
            return;
        }
        var cache = cacheManager.getCache(CacheConfig.HUB_ROUTE_CACHE);
        if (cache != null) {
            cache.evict(routeId);
        }
    }

    private void evictOptimalRouteCacheAll() {
        var cache = cacheManager.getCache(CacheConfig.OPTIMAL_ROUTE_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }
}
