package com.team.deliveryservice.delivery.application.dto.request;

import com.team.deliveryservice.delivery.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeDeliveryStatusRequest(
    @NotNull DeliveryStatus deliveryStatus
) {
}
