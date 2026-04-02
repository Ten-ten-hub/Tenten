package com.team.product_service.stock.presentation;

import com.team.product_service.stock.application.StockService;
import com.team.product_service.stock.application.dto.StockResult;
import com.team.product_service.stock.presentation.dto.StockAdjustRequest;
import com.team.product_service.stock.presentation.dto.StockDeductRequest;
import com.team.product_service.stock.presentation.dto.StockResponse;
import com.team.product_service.stock.presentation.dto.StockRestoreRequest;
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

    // 내부용 - 재고 복원 (주문 취소 시 호출)
    @PostMapping("/restore")
    public ResponseEntity<Void> restoreStock(
        @PathVariable UUID productId,
        @Valid @RequestBody StockRestoreRequest request
    ) {
        stockService.restoreStock(request.toCommand(productId));
        return ResponseEntity.noContent().build();
    }

    // 내부용 - 재고 차감 (주문 서비스 호출)
    @PostMapping("/deduct")
    public ResponseEntity<Void> deductStock(
        @PathVariable UUID productId,
        @Valid @RequestBody StockDeductRequest request
    ) {
        stockService.deductStock(request.toCommand(productId));
        return ResponseEntity.noContent().build();
    }
}
