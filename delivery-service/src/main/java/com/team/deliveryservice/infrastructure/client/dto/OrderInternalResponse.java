package com.team.deliveryservice.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderInternalResponse(
    UUID id,
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    UUID deliveryId,
    LocalDateTime deadlineAt,
    String requestNote,
    String productName,
    String orderStatus
) {
}
