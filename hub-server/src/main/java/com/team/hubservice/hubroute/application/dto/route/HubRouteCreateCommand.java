package com.team.hubservice.hubroute.application.dto.route;

import java.util.UUID;

public record HubRouteCreateCommand(
    UUID departureHubId,
    UUID arrivalHubId,
    Integer duration,
    Double distance
) {}
