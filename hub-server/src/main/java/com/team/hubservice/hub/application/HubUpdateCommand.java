package com.team.hubservice.hub.application;

public record HubUpdateCommand(
    String name,
    Double latitude,
    Double longitude
) {
}
