package com.team.order_service.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    Page<Order> search(UUID orderedBy, UUID supplierCompanyId, UUID receiverCompanyId,
                       OrderStatus orderStatus, Pageable pageable);
}
