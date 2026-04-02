package com.team.hubservice.hubroute.application;

import java.util.UUID;

public record OptimalRouteQuery(
    UUID departureHubId,
    UUID arrivalHubId
) {
}
