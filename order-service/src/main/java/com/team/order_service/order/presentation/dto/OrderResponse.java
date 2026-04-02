package com.team.order_service.order.presentation.dto;

import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    UUID deliveryId,
    LocalDateTime deadlineAt,
    String requestNote,
    BigDecimal totalPrice,
    OrderStatus orderStatus,
    LocalDateTime cancelledAt,
    UUID cancelledBy,
    List<OrderItemResponse> orderItems,
    LocalDateTime createdAt,
    UUID createdBy,
    LocalDateTime updatedAt,
    UUID updatedBy
) {
    public static OrderResponse from(OrderResult result) {
        return new OrderResponse(
            result.id(),
            result.orderedBy(),
            result.supplierCompanyId(),
            result.receiverCompanyId(),
            result.deliveryId(),
            result.deadlineAt(),
            result.requestNote(),
            result.totalPrice(),
            result.orderStatus(),
            result.cancelledAt(),
            result.cancelledBy(),
            result.orderItems().stream().map(OrderItemResponse::from).toList(),
            result.createdAt(),
            result.createdBy(),
            result.updatedAt(),
            result.updatedBy()
        );
    }
}
