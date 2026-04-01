package com.team.deliveryservice.application.delivery;

import com.team.deliveryservice.domain.delivery.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeDeliveryStatusRequest(
    @NotNull DeliveryStatus deliveryStatus
) {
}
