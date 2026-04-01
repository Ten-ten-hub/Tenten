package com.team.deliveryservice.application.deliverymanager;

import com.team.deliveryservice.domain.deliverymanager.DeliveryManagerType;
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
