package com.team.hubservice.hub.dto;

public record HubUpdateRequest(
        String name,
        Double latitude,
        Double longitude
) {
}