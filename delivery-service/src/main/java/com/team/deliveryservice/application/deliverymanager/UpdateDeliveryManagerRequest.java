package com.team.deliveryservice.application.deliverymanager;

import com.team.deliveryservice.domain.deliverymanager.DeliveryManagerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateDeliveryManagerRequest(
    UUID hubId,
    @NotBlank String slackId,
    @NotNull DeliveryManagerType type
) {
}
