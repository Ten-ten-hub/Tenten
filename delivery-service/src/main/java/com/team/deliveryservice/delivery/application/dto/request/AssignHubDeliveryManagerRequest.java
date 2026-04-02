package com.team.deliveryservice.delivery.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignHubDeliveryManagerRequest(
    @NotNull UUID routeLogId,
    @NotNull UUID deliveryManagerId
) {
}
