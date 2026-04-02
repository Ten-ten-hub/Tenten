package com.team.product_service.product.application.dto;

import com.team.product_service.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateCommand(
    UUID productId,
    String name,
    BigDecimal unitPrice,
    String description,
    ProductStatus status
) {
}
