package com.team.product_service.stock.application.dto;

import java.util.UUID;

public record StockAdjustCommand(
    UUID productId,
    int quantity
) {
}
