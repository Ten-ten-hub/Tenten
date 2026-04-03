package com.team.deliveryservice.delivery.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateDeliveryRequest(
    @NotNull UUID orderId,
    @NotNull UUID orderedBy,
    @NotNull UUID supplierCompanyId,
    @NotNull UUID receiverCompanyId,
    @NotNull LocalDateTime deadlineAt,
    String requestNote
) {
}
