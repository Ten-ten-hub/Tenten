package com.team.product_service.stock.application.dto;

import java.util.UUID;

public record StockRestoreCommand(
    UUID productId,
    int amount,
    UUID orderId
) {
}
