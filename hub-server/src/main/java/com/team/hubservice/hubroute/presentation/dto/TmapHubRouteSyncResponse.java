package com.team.hubservice.hubroute.presentation.dto;

import com.team.hubservice.hubroute.application.TmapHubRouteBulkSyncResult;
import com.team.hubservice.hubroute.application.TmapHubRouteSyncResult;
import java.util.List;
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
