package com.team.deliveryservice.application.delivery;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignCompanyDeliveryManagerRequest(
    @NotNull UUID deliveryManagerId
) {
}
