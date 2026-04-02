package com.team.order_service.order.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateCommand(
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    LocalDateTime deadlineAt,
    String requestNote,
    List<OrderItemCommand> orderItems
) {
}
