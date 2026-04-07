package com.team.hubservice.hub.application.dto;

public record HubCreateCommand(
    String name,
    String address,
    Double latitude,
    Double longitude
) {
}
