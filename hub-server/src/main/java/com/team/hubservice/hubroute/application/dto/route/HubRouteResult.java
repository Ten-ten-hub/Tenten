package com.team.hubservice.hubroute.application.dto.route;

import com.team.hubservice.hubroute.domain.HubRoute;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteResult(
    UUID id,
    UUID departureHubId,
    UUID arrivalHubId,
    Integer duration,
    Double distance,
    LocalDateTime createdAt,
    UUID createdBy,
    LocalDateTime updatedAt,
    UUID updatedBy
) {
    public static HubRouteResult from(HubRoute route) {
        return new HubRouteResult(
            route.getId(),
            route.getDepartureHubId(),
            route.getArrivalHubId(),
            route.getDuration(),
            route.getDistance(),
            route.getCreatedAt(),
            route.getCreatedBy(),
            route.getUpdatedAt(),
            route.getUpdatedBy()
        );
    }
}
