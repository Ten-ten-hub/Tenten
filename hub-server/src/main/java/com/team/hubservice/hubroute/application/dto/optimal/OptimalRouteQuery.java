package com.team.hubservice.hubroute.application.dto.optimal;

import java.util.UUID;

public record OptimalRouteQuery(
    UUID departureHubId,
    UUID arrivalHubId
) {
}
