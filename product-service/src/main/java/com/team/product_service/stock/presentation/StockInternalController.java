package com.team.product_service.stock.presentation;

import com.team.product_service.stock.application.StockService;
import com.team.product_service.stock.presentation.dto.StockDeductRequest;
import com.team.product_service.stock.presentation.dto.StockRestoreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/products/{productId}/stock")
public class StockInternalController {

    private final StockService stockService;

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
