package com.team.deliveryservice.application.delivery;

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
