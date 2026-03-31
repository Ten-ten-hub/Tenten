package com.team.product_service.product.application.dto;

import com.team.product_service.product.domain.Product;
import com.team.product_service.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResult(
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
    public static ProductResult from(Product product) {
        return new ProductResult(
            product.getId(),
            product.getCompanyId(),
            product.getHubId(),
            product.getName(),
            product.getStatus(),
            product.getUnitPrice(),
            product.getDescription(),
            product.getCreatedAt(),
            product.getCreatedBy(),
            product.getUpdatedAt(),
            product.getUpdatedBy()
        );
    }
}
