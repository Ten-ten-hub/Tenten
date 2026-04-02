package com.team.product_service.product.infrastructure.client.dto;

public record HubExistsResponse(
    HubExistsData data
) {
    public boolean exists() {
        return data != null && data.exists();
    }

    public record HubExistsData(boolean exists) {
    }
}
