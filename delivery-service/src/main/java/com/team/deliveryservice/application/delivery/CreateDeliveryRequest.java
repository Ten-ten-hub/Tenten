package com.team.deliveryservice.application.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateDeliveryRequest(
    @NotNull UUID orderId,
    @NotNull UUID originHubId,
    @NotNull UUID destinationHubId,
    @NotNull UUID receiverCompanyId,
    @NotBlank String deliveryAddress,
    String deliveryAddressDetail,
    @NotBlank String recipientName,
    @NotBlank String recipientSlackId,
    UUID companyDeliveryManagerId,
    LocalDateTime finalDispatchDeadlineAt
) {
}
