package com.team.order_service.order.application.dto;

import java.util.UUID;

public record OrderItemCommand(
    UUID productId,
    int quantity
) {
}
