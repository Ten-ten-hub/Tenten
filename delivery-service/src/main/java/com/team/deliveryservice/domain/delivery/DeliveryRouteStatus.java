package com.team.deliveryservice.domain.delivery;

public enum DeliveryRouteStatus {
    WAITING_AT_HUB,
    MOVING_BETWEEN_HUBS,
    ARRIVED_AT_DESTINATION_HUB,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
