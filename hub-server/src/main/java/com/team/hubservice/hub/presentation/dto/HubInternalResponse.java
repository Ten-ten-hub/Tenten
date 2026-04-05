package com.team.hubservice.hub.presentation.dto;

import java.util.UUID;

public record HubInternalResponse(
    UUID id,
    String name,
    String address
) {
}
