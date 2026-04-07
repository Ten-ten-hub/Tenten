package com.team.hubservice.hubroute.presentation.dto.sync;

import com.team.hubservice.hubroute.application.dto.sync.TmapHubRouteSyncResult;
import java.util.UUID;

public record TmapHubRouteSyncResponse(
    UUID departureHubId,
    UUID arrivalHubId,
    int durationMinutes,
    double distanceKm,
    String action
) {

    public static TmapHubRouteSyncResponse from(TmapHubRouteSyncResult result) {
        return new TmapHubRouteSyncResponse(
            result.departureHubId(),
            result.arrivalHubId(),
            result.metrics().durationMinutes(),
            result.metrics().distanceKm(),
            result.action().name()
        );
    }
}
