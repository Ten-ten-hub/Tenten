package com.team.order_service.order.application.dto;

import com.team.order_service.order.domain.Order;
import com.team.order_service.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResult(
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
    List<OrderItemResult> orderItems,
    LocalDateTime createdAt,
    UUID createdBy,
    LocalDateTime updatedAt,
    UUID updatedBy
) {
    public static OrderResult from(Order order) {
        return new OrderResult(
            order.getId(),
            order.getOrderedBy(),
            order.getSupplierCompanyId(),
            order.getReceiverCompanyId(),
            order.getDeliveryId(),
            order.getDeadlineAt(),
            order.getRequestNote(),
            order.getTotalPrice(),
            order.getOrderStatus(),
            order.getCancelledAt(),
            order.getCancelledBy(),
            order.getOrderItems().stream().map(OrderItemResult::from).toList(),
            order.getCreatedAt(),
            order.getCreatedBy(),
            order.getUpdatedAt(),
            order.getUpdatedBy()
        );
    }
}
