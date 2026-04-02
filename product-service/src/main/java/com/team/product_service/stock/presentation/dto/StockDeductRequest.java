package com.team.product_service.stock.presentation.dto;

import com.team.product_service.stock.application.dto.StockDeductCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockDeductRequest(

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    Integer amount,

    @NotNull(message = "주문 ID는 필수입니다.")
    UUID orderId
) {
    public StockDeductCommand toCommand(UUID productId) {
        return new StockDeductCommand(productId, amount, orderId);
    }
}
