package com.team.product_service.stock.application.dto;

import com.team.product_service.stock.domain.StockHistoryType;

import java.util.UUID;

public record StockHistoryGetQuery(
    UUID stockId,
    UUID productId,
    UUID orderId,
    StockHistoryType type
) {
}
