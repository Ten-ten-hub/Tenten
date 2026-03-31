package com.team.hubservice.hub.dto;

import com.team.hubservice.hub.domain.Hub;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubResponse(
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
    public static HubResponse from(Hub hub) {
        return new HubResponse(
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
