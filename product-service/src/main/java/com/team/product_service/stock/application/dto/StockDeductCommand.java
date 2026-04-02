package com.team.product_service.stock.application.dto;

import java.util.UUID;

public record StockDeductCommand(
    UUID productId,
    int amount,
    UUID orderId
) {
}
