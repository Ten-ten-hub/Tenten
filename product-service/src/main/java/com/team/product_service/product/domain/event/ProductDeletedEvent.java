package com.team.product_service.product.domain.event;

import java.util.UUID;

public record ProductDeletedEvent(
    UUID productId,
    UUID deletedBy
) {
}
