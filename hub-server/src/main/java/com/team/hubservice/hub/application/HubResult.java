package com.team.hubservice.hub.application;

import com.team.hubservice.hub.domain.Hub;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubResult(
    UUID id,
    String name,
    String address,
    Double latitude,
    Double longitude,
    LocalDateTime createdAt,
    UUID createdBy,
    LocalDateTime updatedAt,
    UUID updatedBy
) {
    public static HubResult from(Hub hub) {
        return new HubResult(
            hub.getId(),
            hub.getName(),
            hub.getAddress(),
            hub.getLatitude(),
            hub.getLongitude(),
            hub.getCreatedAt(),
            hub.getCreatedBy(),
            hub.getUpdatedAt(),
            hub.getUpdatedBy()
        );
    }
}
