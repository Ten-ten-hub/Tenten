package com.team.product_service.product.presentation.dto;

import com.team.product_service.product.application.dto.ProductCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreateRequest(
    @NotBlank String name,
    @NotNull UUID companyId,
    @NotNull UUID hubId,
    @NotNull @Positive BigDecimal unitPrice,
    String description
) {
    public ProductCreateCommand toCommand() {
        return new ProductCreateCommand(name, companyId, hubId, unitPrice, description);
    }
}
