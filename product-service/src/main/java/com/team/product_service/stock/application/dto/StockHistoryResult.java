package com.team.product_service.stock.application.dto;

import com.team.product_service.stock.domain.StockHistory;
import com.team.product_service.stock.domain.StockHistoryType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockHistoryResult(
    UUID id,
    UUID stockId,
    StockHistoryType type,
    Integer quantityChange,
    Integer quantityAfter,
    UUID orderId,
    LocalDateTime createdAt,
    UUID createdBy
) {
    public static StockHistoryResult from(StockHistory stockHistory) {
        return new StockHistoryResult(
            stockHistory.getId(),
            stockHistory.getStockId(),
            stockHistory.getType(),
            stockHistory.getQuantityChange(),
            stockHistory.getQuantityAfter(),
            stockHistory.getOrderId(),
            stockHistory.getCreatedAt(),
            stockHistory.getCreatedBy()
        );
    }
}
