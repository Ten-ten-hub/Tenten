package com.team.product_service.stock.domain;

import com.team.product_service.stock.application.dto.StockHistoryGetQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository {

    Stock save(Stock stock);

    Optional<Stock> findByProductIdAndDeletedAtIsNull(UUID productId);

    StockHistory saveHistory(StockHistory stockHistory);

    Page<StockHistory> searchHistory(StockHistoryGetQuery query, Pageable pageable);
}
