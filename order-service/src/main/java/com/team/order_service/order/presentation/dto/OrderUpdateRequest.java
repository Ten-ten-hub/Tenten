package com.team.order_service.order.presentation.dto;

import com.team.order_service.order.application.dto.OrderUpdateCommand;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdateRequest(
    LocalDateTime deadlineAt,
    String requestNote
) {
    public OrderUpdateCommand toCommand(UUID orderId) {
        return new OrderUpdateCommand(orderId, deadlineAt, requestNote);
    }
}
