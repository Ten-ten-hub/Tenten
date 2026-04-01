package com.team.product_service.stock.application;

import com.team.product_service.product.domain.event.ProductCreatedEvent;
import com.team.product_service.product.domain.event.ProductDeletedEvent;
import com.team.product_service.stock.domain.Stock;
import com.team.product_service.stock.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StockEventHandler {

    private final StockRepository stockRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.MANDATORY)
    public void handleProductCreated(ProductCreatedEvent event) {
        Stock stock = Stock.create(event.productId(), event.hubId());
        stockRepository.save(stock);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.MANDATORY)
    public void handleProductDeleted(ProductDeletedEvent event) {
        stockRepository.findByProductIdAndDeletedAtIsNull(event.productId())
            .ifPresent(stock -> stock.softDelete(event.deletedBy()));
    }

}
