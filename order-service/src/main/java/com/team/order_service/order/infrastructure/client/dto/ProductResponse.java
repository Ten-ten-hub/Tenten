package com.team.order_service.order.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    BigDecimal unitPrice
) {
}
