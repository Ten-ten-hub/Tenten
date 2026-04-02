package com.team.hubservice.hubroute.application;

public record HubRouteUpdateCommand(
    Integer duration,
    Double distance
) {}
