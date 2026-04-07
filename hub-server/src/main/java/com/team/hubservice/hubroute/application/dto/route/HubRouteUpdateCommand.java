package com.team.hubservice.hubroute.application.dto.route;

public record HubRouteUpdateCommand(
    Integer duration,
    Double distance
) {}
