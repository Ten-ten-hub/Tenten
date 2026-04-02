package com.team.hubservice.hubroute.application;

import java.util.List;
import java.util.UUID;

public record OptimalRouteResult(
    UUID departureHubId,
    UUID arrivalHubId,
    Integer totalDuration,
    Double totalDistance,
    List<RoutePathInfo> routePathList
) {
}
