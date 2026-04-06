package com.team.deliveryservice.deliverymanager.application.dto.request;

import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeliveryManagerRequest(
    @NotNull UUID userId,
    @NotNull UUID hubId,
    @NotBlank String slackId,
    @NotNull DeliveryManagerType type
) {
}
