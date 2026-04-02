package com.team.product_service.stock.application;

import com.team.product_service.stock.application.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockService {

    StockResult getStock(UUID productId);

    // 관리자용 - 재고 조정
    StockResult adjustStock(StockAdjustCommand command);

    // 내부용 - 주문 서비스 호출
    void deductStock(StockDeductCommand command);

    void restoreStock(StockRestoreCommand command);

    Page<StockHistoryResult> getStockHistories(StockHistoryGetQuery query, Pageable pageable);

}
