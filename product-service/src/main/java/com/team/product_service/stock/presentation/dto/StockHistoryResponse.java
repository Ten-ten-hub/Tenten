package com.team.product_service.stock.presentation.dto;

import com.team.product_service.stock.application.dto.StockHistoryResult;
import com.team.product_service.stock.domain.StockHistoryType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockHistoryResponse(
    UUID id,
    UUID stock_id,
    UUID orderId,
    StockHistoryType type,
    int quantityChange,
    int quantityAfter,
    LocalDateTime createdAt,
    UUID createdBy
) {
    public static StockHistoryResponse from(StockHistoryResult result) {
        return new StockHistoryResponse(
            result.id(),
            result.stockId(),
            result.orderId(),
            result.type(),
            result.quantityChange(),
            result.quantityAfter(),
            result.createdAt(),
            result.createdBy()
        );
    }
}
