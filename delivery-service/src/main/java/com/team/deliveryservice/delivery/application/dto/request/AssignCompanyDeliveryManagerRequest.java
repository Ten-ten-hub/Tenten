package com.team.deliveryservice.delivery.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignCompanyDeliveryManagerRequest(
    @NotNull UUID deliveryManagerId
) {
}
