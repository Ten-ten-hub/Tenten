package com.team.order_service.order.application.dto;

import com.team.order_service.order.domain.OrderStatus;

import java.util.UUID;

public record OrderGetQuery(
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    OrderStatus orderStatus
) {
}
