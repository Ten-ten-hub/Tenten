package com.team.hubservice.hubroute.application;

import java.util.UUID;

public record HubRouteCreateCommand(
    UUID departureHubId,
    UUID arrivalHubId,
    Integer duration,
    Double distance
) {}
