package com.team.order_service.order.infrastructure;

import com.team.order_service.order.domain.Order;
import com.team.order_service.order.domain.OrderRepository;
import com.team.order_service.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findByIdAndDeletedAtIsNull(UUID id) {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Order> search(UUID orderedBy, UUID supplierCompanyId, UUID receiverCompanyId,
                              OrderStatus orderStatus, Pageable pageable) {
        return orderJpaRepository.search(orderedBy, supplierCompanyId, receiverCompanyId, orderStatus, pageable);
    }
}
