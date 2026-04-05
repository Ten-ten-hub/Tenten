package com.team.aiservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AiRequest(
    @NotNull UUID orderId,
    @NotBlank String productName,
    @NotNull UUID originHubId,
    @NotBlank String originHubName,
    @NotBlank String originAddress,
    @NotNull UUID destinationHubId,
    @NotBlank String destinationHubName,
    @NotBlank String destinationAddress,
    @NotBlank String orderRequestDetails,
    @NotNull UUID receiverId,
    @NotBlank String receiverSlackId,
    @NotBlank String workingHours
) {
}
