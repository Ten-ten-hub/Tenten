package com.team.hubservice.hub.presentation.dto;

import com.team.hubservice.hub.application.dto.HubResult;
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
    public static HubResponse from(HubResult result) {
        return new HubResponse(
                result.id(),
                result.name(),
                result.address(),
                result.latitude(),
                result.longitude(),
                result.createdAt(),
                result.createdBy(),
                result.updatedAt(),
                result.updatedBy()
        );
    }
}
