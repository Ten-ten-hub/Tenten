package com.team.order_service.order.infrastructure.client.dto;

// delivery랑 응답 형식 맞추기 위한 임시 dto
public record DeliveryApiResponse(
    boolean success,
    DeliveryResponse data,
    String code,
    String message
) {
}
