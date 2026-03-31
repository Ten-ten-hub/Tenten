package com.team.product_service.product.application.dto;

import com.team.product_service.product.domain.ProductStatus;

import java.util.UUID;

public record ProductGetQuery(
        String name,
        UUID companyId,
        UUID hubId,
        ProductStatus status
) {
}
