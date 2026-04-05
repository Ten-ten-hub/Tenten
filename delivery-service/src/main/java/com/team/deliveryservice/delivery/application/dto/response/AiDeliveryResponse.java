package com.team.deliveryservice.delivery.application.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiDeliveryResponse {

    private UUID orderId;

    private UUID originHubId;
    private UUID destinationHubId;

    private String originHubName;
    private String destinationHubName;

    private String originAddress;
    private String destinationAddress;

    private String productName;
    private String orderRequestDetails;

    private String receiverSlackId;
}
