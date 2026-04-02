package com.team.order_service.order.presentation.dto;

import com.team.order_service.order.application.dto.OrderItemResult;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    UUID productId,
    String productNameSnapshot,
    BigDecimal unitPriceSnapshot,
    Integer quantity
) {
    public static OrderItemResponse from(OrderItemResult result) {
        return new OrderItemResponse(
            result.id(),
            result.productId(),
            result.productNameSnapshot(),
            result.unitPriceSnapshot(),
            result.quantity()
        );
    }
}
