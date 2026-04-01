package com.team.deliveryservice.delivery.application.service;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryCreateCommand(
    UUID orderId,
    UUID originHubId,
    UUID destinationHubId,
    UUID receiverCompanyId,
    String deliveryAddress,
    String deliveryAddressDetail,
    String recipientName,
    String recipientSlackId,
    UUID companyDeliveryManagerId,
    LocalDateTime finalDispatchDeadlineAt,
    UUID createdBy
) {
}
