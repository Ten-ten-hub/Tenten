package com.team.order_service.order.infrastructure.client.dto;

import java.util.UUID;

public record StockDeductRequest(
    int amount,
    UUID orderId
) {
}
