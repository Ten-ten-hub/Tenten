package com.team.deliveryservice.infrastructure.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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
