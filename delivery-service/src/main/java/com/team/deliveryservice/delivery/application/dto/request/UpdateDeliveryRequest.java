package com.team.deliveryservice.delivery.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record UpdateDeliveryRequest(
    @NotBlank String deliveryAddress,
    String deliveryAddressDetail,
    @NotBlank String recipientName,
    @NotBlank String recipientSlackId,
    UUID companyDeliveryManagerId
) {
}
