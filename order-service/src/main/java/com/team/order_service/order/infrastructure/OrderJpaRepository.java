package com.team.order_service.order.infrastructure;

import com.team.order_service.order.domain.Order;
import com.team.order_service.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    // 나중에 QueryDSL로 교체
    @Query("""
        SELECT o FROM Order o
        WHERE o.deletedAt IS NULL
          AND (:orderedBy IS NULL OR o.orderedBy = :orderedBy)
          AND (:supplierCompanyId IS NULL OR o.supplierCompanyId = :supplierCompanyId)
          AND (:receiverCompanyId IS NULL OR o.receiverCompanyId = :receiverCompanyId)
          AND (CAST(:orderStatus AS string) IS NULL OR o.orderStatus = :orderStatus)
        """)
    Page<Order> search(
        @Param("orderedBy") UUID orderedBy,
        @Param("supplierCompanyId") UUID supplierCompanyId,
        @Param("receiverCompanyId") UUID receiverCompanyId,
        @Param("orderStatus") OrderStatus orderStatus,
        Pageable pageable
    );
}
