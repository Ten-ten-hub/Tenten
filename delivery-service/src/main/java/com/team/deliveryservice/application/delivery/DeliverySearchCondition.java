package com.team.deliveryservice.application.delivery;

import com.team.deliveryservice.domain.delivery.DeliveryStatus;
import java.util.UUID;

public record DeliverySearchCondition(
    UUID orderId,
    DeliveryStatus deliveryStatus,
    UUID originHubId,
    UUID destinationHubId,
    UUID receiverCompanyId,
    UUID companyDeliveryManagerId,
    Integer page,
    Integer size,
    String sortBy,
    String direction
) {
}
