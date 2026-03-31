package com.team.product_service.product.presentation.dto;

import com.team.product_service.product.application.dto.ProductUpdateCommand;
import com.team.product_service.product.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateRequest(

    @Size(max = 150, message = "상품명은 150자를 초과할 수 없습니다")
    String name,

    @DecimalMin(value = "0.0", inclusive = false, message = "단가는 0보다 커야 합니다")
    @Digits(integer = 10, fraction = 2, message = "단가는 정수 10자리, 소수 2자리를 초과할 수 없습니다")
    BigDecimal unitPrice,

    @Size(max = 500, message = "상품 설명은 500자를 초과할 수 없습니다")
    String description,

    ProductStatus status
) {
    public ProductUpdateCommand toCommand(UUID productId) {
        return new ProductUpdateCommand(productId, name, unitPrice, description, status);
    }
}
