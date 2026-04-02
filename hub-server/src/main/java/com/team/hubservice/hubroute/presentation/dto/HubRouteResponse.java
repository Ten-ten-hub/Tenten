package com.team.hubservice.hubroute.presentation.dto;

import com.team.hubservice.hubroute.application.HubRouteResult;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteResponse(
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
    public static HubRouteResponse from(HubRouteResult result) {
        return new HubRouteResponse(
            result.id(),
            result.departureHubId(),
            result.arrivalHubId(),
            result.duration(),
            result.distance(),
            result.createdAt(),
            result.createdBy(),
            result.updatedAt(),
            result.updatedBy()
        );
    }
}
