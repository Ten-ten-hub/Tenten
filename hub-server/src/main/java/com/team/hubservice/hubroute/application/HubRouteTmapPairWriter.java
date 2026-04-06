package com.team.hubservice.hubroute.application;

import com.team.hubservice.hubroute.domain.HubRoute;
import com.team.hubservice.hubroute.domain.HubRouteRepository;
import com.team.hubservice.hubroute.infrastructure.tmap.TmapRouteMetrics;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HubRouteTmapPairWriter {

    private final HubRouteRepository hubRouteRepository;

    public HubRouteTmapPairWriter(HubRouteRepository hubRouteRepository) {
        this.hubRouteRepository = hubRouteRepository;
    }

    @Transactional
    public TmapHubRouteSyncResult upsert(UUID departureHubId, UUID arrivalHubId, TmapRouteMetrics metrics) {
        return hubRouteRepository.findByDepartureHubIdAndArrivalHubId(departureHubId, arrivalHubId)
            .map(route -> {
                route.update(metrics.durationMinutes(), metrics.distanceKm());
                return TmapHubRouteSyncResult.updated(departureHubId, arrivalHubId, metrics);
            })
            .orElseGet(() -> createRoute(departureHubId, arrivalHubId, metrics));
    }

    private TmapHubRouteSyncResult createRoute(UUID departureHubId, UUID arrivalHubId, TmapRouteMetrics metrics) {
        HubRoute route = HubRoute.create(
            departureHubId,
            arrivalHubId,
            metrics.durationMinutes(),
            metrics.distanceKm()
        );
        try {
            hubRouteRepository.save(route);
            return TmapHubRouteSyncResult.created(departureHubId, arrivalHubId, metrics);
        } catch (DataIntegrityViolationException e) {
            return hubRouteRepository.findByDepartureHubIdAndArrivalHubId(departureHubId, arrivalHubId)
                .map(existing -> {
                    existing.update(metrics.durationMinutes(), metrics.distanceKm());
                    return TmapHubRouteSyncResult.updated(departureHubId, arrivalHubId, metrics);
                })
                .orElseThrow(() -> e);
        }
    }
}
