package com.team.product_service.product.domain.event;

import java.util.UUID;

public record ProductCreatedEvent(
    UUID productId,
    UUID hubId
) {
}
