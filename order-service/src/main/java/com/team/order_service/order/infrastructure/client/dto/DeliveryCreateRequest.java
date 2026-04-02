package com.team.order_service.order.infrastructure.client.dto;

import java.util.UUID;

public record DeliveryCreateRequest(
    UUID orderId,
    UUID supplierCompanyId,
    UUID receiverCompanyId
) {
}
