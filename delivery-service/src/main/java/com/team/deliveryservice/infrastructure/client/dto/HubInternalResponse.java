package com.team.deliveryservice.infrastructure.client.dto;

import java.util.UUID;

public record HubInternalResponse(
    UUID id,
    String name,
    String address
) {
}
