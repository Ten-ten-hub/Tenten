package com.team.aiservice.application.dto;

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
    UUID receiverId,
    String receiverSlackId,
    String workingHours
) {
}
