package com.team.product_service.stock.presentation;

import com.team.product_service.stock.application.StockService;
import com.team.product_service.stock.application.dto.StockResult;
import com.team.product_service.stock.presentation.dto.StockAdjustRequest;
import com.team.product_service.stock.presentation.dto.StockResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/{productId}/stock")
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<StockResponse> getStock(
            @PathVariable UUID productId
    ) {
        StockResult result = stockService.getStock(productId);
        return ResponseEntity.ok(StockResponse.from(result));
    }

    // 관리자용 - 재고 조정
    @PatchMapping
    public ResponseEntity<StockResponse> adjustStock(
            @RequestHeader("X-User-Id") UUID requestUserId,
            @RequestHeader("X-User-Role") String requestUserRole,
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustRequest request
    ) {
        StockResult result = stockService.adjustStock(request.toCommand(productId));
        return ResponseEntity.ok(StockResponse.from(result));
    }
}
