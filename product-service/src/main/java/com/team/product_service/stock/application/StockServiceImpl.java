package com.team.product_service.stock.application;

import com.team.common.exception.BusinessException;
import com.team.product_service.global.exception.ProductErrorCode;
import com.team.product_service.stock.application.dto.*;
import com.team.product_service.stock.domain.Stock;
import com.team.product_service.stock.domain.StockHistory;
import com.team.product_service.stock.domain.StockHistoryType;
import com.team.product_service.stock.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;

    @Override
    public StockResult getStock(UUID productId) {
        Stock stock = findActiveStockByProductId(productId);
        return StockResult.from(stock);
    }

    @Override
    @Transactional
    public StockResult adjustStock(StockAdjustCommand command) {
        Stock stock = findActiveStockByProductId(command.productId());
        int before = stock.getQuantity();

        stock.adjust(command.quantity());

        int change = stock.getQuantity() - before;
        stockRepository.saveHistory(
            StockHistory.of(stock.getId(), StockHistoryType.ADJUSTMENT, change, stock.getQuantity(), null)
        );

        return StockResult.from(stock);
    }

    @Override
    @Transactional
    public void deductStock(StockDeductCommand command) {
        Stock stock = findActiveStockByProductId(command.productId());

        stock.decrease(command.amount());

        stockRepository.saveHistory(
            StockHistory.of(stock.getId(), StockHistoryType.OUTBOUND, -command.amount(), stock.getQuantity(), command.orderId())
        );
    }

    @Override
    @Transactional
    public void restoreStock(StockRestoreCommand command) {
        Stock stock = findActiveStockByProductId(command.productId());

        stock.increase(command.amount());

        stockRepository.saveHistory(
            StockHistory.of(stock.getId(), StockHistoryType.RESTORE, command.amount(), stock.getQuantity(), command.orderId())
        );
    }


    @Override
    public Page<StockHistoryResult> getStockHistories(StockHistoryGetQuery query, Pageable pageable) {
        UUID stockId = query.stockId();
        if (query.productId() != null && stockId == null) {
            Optional<Stock> stock = stockRepository.findByProductIdAndDeletedAtIsNull(query.productId());
            if (stock.isEmpty()) {
                return Page.empty(pageable);
            }
            stockId = stock.get().getId();
        }
        StockHistoryGetQuery resolvedQuery = new StockHistoryGetQuery(
            stockId, query.productId(), query.orderId(), query.type()
        );

        return stockRepository.searchHistory(stockId, query.orderId(), query.type(), pageable)
            .map(StockHistoryResult::from);
    }


    private Stock findActiveStockByProductId(UUID productId) {
        return stockRepository.findByProductIdAndDeletedAtIsNull(productId)
            .orElseThrow(() -> new BusinessException(ProductErrorCode.STOCK_NOT_FOUND));
    }
}
