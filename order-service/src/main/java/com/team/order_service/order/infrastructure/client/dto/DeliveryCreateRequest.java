package com.team.order_service.order.infrastructure.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryCreateRequest(
    UUID orderId,
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    LocalDateTime deadlineAt,
    String requestNote
) {
}
