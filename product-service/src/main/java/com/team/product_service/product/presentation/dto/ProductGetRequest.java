package com.team.product_service.product.presentation.dto;

import com.team.product_service.product.application.dto.ProductGetQuery;
import com.team.product_service.product.domain.ProductStatus;

import java.util.UUID;

public record ProductGetRequest(
    String name,
    UUID companyId,
    UUID hubId,
    ProductStatus status
) {
    public ProductGetQuery toQuery() {
        return new ProductGetQuery(name, companyId, hubId, status);
    }
}
