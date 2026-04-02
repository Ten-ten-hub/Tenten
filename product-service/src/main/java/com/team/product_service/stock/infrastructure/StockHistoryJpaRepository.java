package com.team.product_service.stock.infrastructure;

import com.team.product_service.stock.domain.StockHistory;
import com.team.product_service.stock.domain.StockHistoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StockHistoryJpaRepository extends JpaRepository<StockHistory, UUID> {

    // 나중에 QueryDSL로 교체
    @Query("""
        SELECT h FROM StockHistory h
        WHERE h.deletedAt IS NULL
          AND (:stockId IS NULL OR h.stockId = :stockId)
          AND (:orderId IS NULL OR h.orderId = :orderId)
          AND (CAST(:type AS string) IS NULL OR h.type = :type)
        """)
    Page<StockHistory> search(
        @Param("stockId") UUID stockId,
        @Param("orderId") UUID orderId,
        @Param("type") StockHistoryType type,
        Pageable pageable
    );
}
