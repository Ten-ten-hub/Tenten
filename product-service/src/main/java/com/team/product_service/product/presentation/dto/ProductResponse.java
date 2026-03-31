package com.team.product_service.product.presentation.dto;

import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID companyId,
    UUID hubId,
    String name,
    ProductStatus status,
    BigDecimal unitPrice,
    String description,
    LocalDateTime createdAt,
    UUID createdBy,
    LocalDateTime updatedAt,
    UUID updatedBy
) {
    public static ProductResponse from(ProductResult result) {
        return new ProductResponse(
            result.id(),
            result.companyId(),
            result.hubId(),
            result.name(),
            result.status(),
            result.unitPrice(),
            result.description(),
            result.createdAt(),
            result.createdBy(),
            result.updatedAt(),
            result.updatedBy()
        );
    }
}
