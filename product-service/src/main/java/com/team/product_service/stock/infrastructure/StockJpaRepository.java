package com.team.product_service.stock.infrastructure;

import com.team.product_service.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockJpaRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductIdAndDeletedAtIsNull(UUID productId);
}
