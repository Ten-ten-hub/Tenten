package com.team.product_service.product.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreateCommand(
    String name,
    UUID companyId,
    UUID hubId,
    BigDecimal unitPrice,
    String description
) {
}
