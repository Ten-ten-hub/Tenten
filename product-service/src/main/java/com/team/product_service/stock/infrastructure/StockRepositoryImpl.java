package com.team.product_service.stock.infrastructure;

import com.team.product_service.stock.domain.Stock;
import com.team.product_service.stock.domain.StockHistory;
import com.team.product_service.stock.domain.StockHistoryType;
import com.team.product_service.stock.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepository {

    private final StockJpaRepository stockJpaRepository;
    private final StockHistoryJpaRepository stockHistoryJpaRepository;

    @Override
    public Stock save(Stock stock) {
        return stockJpaRepository.save(stock);
    }

    @Override
    public StockHistory saveHistory(StockHistory stockHistory) {
        return stockHistoryJpaRepository.save(stockHistory);
    }

    @Override
    public Optional<Stock> findByProductIdAndDeletedAtIsNull(UUID productId) {
        return stockJpaRepository.findByProductIdAndDeletedAtIsNull(productId);
    }

    @Override
    public Page<StockHistory> searchHistory(UUID stockId, UUID orderId, StockHistoryType type, Pageable pageable) {
        return stockHistoryJpaRepository.search(stockId, orderId, type, pageable);
    }
}
