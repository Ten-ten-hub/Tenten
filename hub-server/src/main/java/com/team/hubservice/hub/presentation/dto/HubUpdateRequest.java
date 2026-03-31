package com.team.hubservice.hub.presentation.dto;

public record HubUpdateRequest(
        String name,
        Double latitude,
        Double longitude
) {
}
