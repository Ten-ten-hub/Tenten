package com.team.product_service.stock.presentation.dto;

import com.team.product_service.stock.application.dto.StockAdjustCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockAdjustRequest(

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 0, message = "수량은 0 이상이어야 합니다")
    Integer quantity

) {
    public StockAdjustCommand toCommand(UUID productId) {
        return new StockAdjustCommand(productId, quantity);
    }
}
