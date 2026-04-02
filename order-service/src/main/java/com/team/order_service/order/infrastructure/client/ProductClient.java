package com.team.order_service.order.infrastructure.client;

import com.team.order_service.order.infrastructure.client.dto.ProductResponse;
import com.team.order_service.order.infrastructure.client.dto.StockDeductRequest;
import com.team.order_service.order.infrastructure.client.dto.StockRestoreRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    ProductResponse getProduct(
            @RequestHeader("X-User-Id") UUID requestUserId,
            @PathVariable UUID productId
    );

    @PostMapping("/internal/v1/products/{productId}/stock/deduct")
    void deductStock(
            @RequestHeader("X-User-Id") UUID requestUserId,
            @PathVariable UUID productId,
            @RequestBody StockDeductRequest request
    );

    @PostMapping("/internal/v1/products/{productId}/stock/restore")
    void restoreStock(
            @RequestHeader("X-User-Id") UUID requestUserId,
            @PathVariable UUID productId,
            @RequestBody StockRestoreRequest request
    );
}
