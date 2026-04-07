package com.team.hubservice.hubroute.application.dto.sync;

import com.team.hubservice.hubroute.infrastructure.tmap.TmapRouteMetrics;
import java.util.UUID;

public record TmapHubRouteSyncResult(
    UUID departureHubId,
    UUID arrivalHubId,
    TmapRouteMetrics metrics,
    SyncAction action
) {

    public enum SyncAction {
        CREATED,
        UPDATED
    }

    public static TmapHubRouteSyncResult created(UUID departureHubId, UUID arrivalHubId, TmapRouteMetrics metrics) {
        return new TmapHubRouteSyncResult(departureHubId, arrivalHubId, metrics, SyncAction.CREATED);
    }

    public static TmapHubRouteSyncResult updated(UUID departureHubId, UUID arrivalHubId, TmapRouteMetrics metrics) {
        return new TmapHubRouteSyncResult(departureHubId, arrivalHubId, metrics, SyncAction.UPDATED);
    }
}
