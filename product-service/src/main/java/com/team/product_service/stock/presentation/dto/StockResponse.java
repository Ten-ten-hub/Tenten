package com.team.product_service.stock.presentation.dto;

import com.team.product_service.stock.application.dto.StockResult;
import com.team.product_service.stock.domain.StockStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockResponse(
    UUID id,
    UUID productId,
    Integer quantity,
    StockStatus status,
    LocalDateTime createdAt,
    UUID createdBy,
    LocalDateTime updatedAt,
    UUID updatedBy
) {
    public static StockResponse from(StockResult result) {
        return new StockResponse(
            result.id(),
            result.productId(),
            result.quantity(),
            result.status(),
            result.createdAt(),
            result.createdBy(),
            result.updatedAt(),
            result.updatedBy()
        );
    }
}
