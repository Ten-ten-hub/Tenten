package com.team.hubservice.hub.application.dto;

public record HubUpdateCommand(
    String name,
    Double latitude,
    Double longitude
) {
}
