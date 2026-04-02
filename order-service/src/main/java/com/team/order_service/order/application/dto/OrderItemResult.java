package com.team.order_service.order.application.dto;

import com.team.order_service.order.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResult(
    UUID id,
    UUID productId,
    String productNameSnapshot,
    BigDecimal unitPriceSnapshot,
    Integer quantity
) {
    public static OrderItemResult from(OrderItem orderItem) {
        return new OrderItemResult(
            orderItem.getId(),
            orderItem.getProductId(),
            orderItem.getProductNameSnapshot(),
            orderItem.getUnitPriceSnapshot(),
            orderItem.getQuantity()
        );
    }
}
