package com.team.order_service.order.infrastructure.client.dto;

import java.util.UUID;

public record StockRestoreRequest(
    int amount,
    UUID orderId
) {
}
