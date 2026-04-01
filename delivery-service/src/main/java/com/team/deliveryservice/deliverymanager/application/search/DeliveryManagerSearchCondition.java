package com.team.deliveryservice.deliverymanager.application.search;

import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import java.util.UUID;

public record DeliveryManagerSearchCondition(
    UUID hubId,
    DeliveryManagerType type,
    Integer page,
    Integer size,
    String sortBy,
    String direction
) {
}
