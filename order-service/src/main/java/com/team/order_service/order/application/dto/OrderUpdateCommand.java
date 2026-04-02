package com.team.order_service.order.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdateCommand(
    UUID orderId,
    LocalDateTime deadlineAt,
    String requestNote
) {
}
