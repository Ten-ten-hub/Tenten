package com.team.hubservice.hubroute.presentation.dto.optimal;

import com.team.hubservice.hubroute.application.dto.optimal.RoutePathInfo;
import java.util.UUID;

public record RoutePathResponse(
    Integer sequence,
    UUID departureHubId,
    UUID arrivalHubId,
    Integer duration,
    Double distance
) {
    public static RoutePathResponse from(RoutePathInfo info) {
        return new RoutePathResponse(
            info.sequence(),
            info.departureHubId(),
            info.arrivalHubId(),
            info.duration(),
            info.distance()
        );
    }
}
