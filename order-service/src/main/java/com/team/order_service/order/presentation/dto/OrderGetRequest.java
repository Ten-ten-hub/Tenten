package com.team.order_service.order.presentation.dto;

import com.team.order_service.order.application.dto.OrderGetQuery;
import com.team.order_service.order.domain.OrderStatus;

import java.util.UUID;

public record OrderGetRequest(
    UUID orderedBy,
    UUID supplierCompanyId,
    UUID receiverCompanyId,
    OrderStatus orderStatus
) {
    public OrderGetQuery toQuery() {
        return new OrderGetQuery(orderedBy, supplierCompanyId, receiverCompanyId, orderStatus);
    }
}
