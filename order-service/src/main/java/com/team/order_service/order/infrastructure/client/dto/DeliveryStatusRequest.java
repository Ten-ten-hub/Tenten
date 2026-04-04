package com.team.order_service.order.infrastructure.client.dto;

public record DeliveryStatusRequest(
    // 배송 상태 enum을 안가지고 있어서 일단 String으로 했는데
    // 주문 상태랑 맞추거나 해야할 듯
    String deliveryStatus
) {
}
