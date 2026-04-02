package com.team.product_service.stock.application.dto;

import com.team.product_service.stock.domain.Stock;
import com.team.product_service.stock.domain.StockStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockResult(
    UUID id,
    UUID productId,
    Integer quantity,
    StockStatus status,
    LocalDateTime createdAt,
    UUID createdBy,
    LocalDateTime updatedAt,
    UUID updatedBy
) {
    public static StockResult from(Stock stock) {
        return new StockResult(
            stock.getId(),
            stock.getProductId(),
            stock.getQuantity(),
            stock.getStatus(),
            stock.getCreatedAt(),
            stock.getCreatedBy(),
            stock.getUpdatedAt(),
            stock.getUpdatedBy()
        );
    }
}
