package com.team.product_service.stock.presentation;

import com.team.product_service.global.dto.PageResponse;
import com.team.product_service.stock.application.StockService;
import com.team.product_service.stock.application.dto.StockHistoryResult;
import com.team.product_service.stock.presentation.dto.StockHistoryGetRequest;
import com.team.product_service.stock.presentation.dto.StockHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks/histories")
public class StockHistoryController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<PageResponse<StockHistoryResponse>> getStockHistories(
            @ModelAttribute StockHistoryGetRequest request,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        Pageable validatedPageable = validatePageSize(pageable);
        Page<StockHistoryResult> results = stockService.getStockHistories(request.toQuery(), validatedPageable);
        return ResponseEntity.ok(PageResponse.from(results.map(StockHistoryResponse::from)));
    }

    private Pageable validatePageSize(Pageable pageable) {
        int size = pageable.getPageSize();
        if (size != 10 && size != 30 && size != 50) {
            return PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());
        }
        return pageable;
    }
}
