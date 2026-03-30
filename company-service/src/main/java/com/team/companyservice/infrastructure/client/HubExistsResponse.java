package com.team.companyservice.infrastructure.client;

import java.util.UUID;

public record HubExistsResponse(
        boolean success,
        HubExistsData data,
        String code,
        String message
) {
    public boolean exists() {
        return data != null && data.exists();
    }

    public record HubExistsData(
            UUID hubId,
            boolean exists
    ) {
    }
}