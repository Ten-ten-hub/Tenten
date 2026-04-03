package com.team.deliveryservice.delivery.application.service;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryCreateCommand(
    UUID orderId,
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    LocalDateTime deadlineAt,
    String requestNote
) {
}
