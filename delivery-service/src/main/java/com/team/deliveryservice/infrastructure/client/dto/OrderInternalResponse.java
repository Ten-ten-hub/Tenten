package com.team.deliveryservice.infrastructure.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderInternalResponse(
    UUID id,
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    UUID deliveryId,
    LocalDateTime deadlineAt,
    String orderStatus
) {
}
