package com.team.hubservice.hubroute.application.dto.optimal;

import java.util.UUID;

public record RoutePathInfo(
    Integer sequence,
    UUID departureHubId,
    UUID arrivalHubId,
    Integer duration,
    Double distance
) {
}
