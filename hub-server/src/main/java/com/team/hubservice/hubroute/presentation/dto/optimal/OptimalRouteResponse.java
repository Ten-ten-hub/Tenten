package com.team.hubservice.hubroute.presentation.dto.optimal;

import com.team.hubservice.hubroute.application.dto.optimal.OptimalRouteResult;
import java.util.List;
import java.util.UUID;

public record OptimalRouteResponse(
    UUID departureHubId,
    UUID arrivalHubId,
    Integer totalDuration,
    Double totalDistance,
    List<RoutePathResponse> routePathList
) {
    public static OptimalRouteResponse from(OptimalRouteResult result) {
        return new OptimalRouteResponse(
            result.departureHubId(),
            result.arrivalHubId(),
            result.totalDuration(),
            result.totalDistance(),
            result.routePathList().stream().map(RoutePathResponse::from).toList()
        );
    }
}
