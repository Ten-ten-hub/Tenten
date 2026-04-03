package com.team.deliveryservice.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

public record OptimalRouteResponseWrapper(
    int code,
    String message,
    OptimalRouteResponse data
) {
    public record OptimalRouteResponse(
        UUID departureHubId,
        UUID arrivalHubId,
        Integer totalDuration,
        Double totalDistance,
        List<RoutePathResponse> routePathList
    ) {
    }

    public record RoutePathResponse(
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        Integer duration,
        Double distance
    ) {
    }
}
