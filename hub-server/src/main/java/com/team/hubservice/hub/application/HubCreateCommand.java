package com.team.hubservice.hub.application;

public record HubCreateCommand(
    String name,
    String address,
    Double latitude,
    Double longitude
) {
}
