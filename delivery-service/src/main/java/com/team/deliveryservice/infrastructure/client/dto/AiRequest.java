package com.team.deliveryservice.infrastructure.client.dto;

import java.util.UUID;

public record AiRequest(
    UUID orderId,
    String productName,
    UUID originHubId,
    String originHubName,
    String originAddress,
    UUID destinationHubId,
    String destinationHubName,
    String destinationAddress,
    String orderRequestDetails,
    String receiverSlackId,
    String workingHours
) {
}
