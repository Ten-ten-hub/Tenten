package com.team.deliveryservice.delivery.application.search;

import com.team.deliveryservice.delivery.domain.DeliveryStatus;
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
