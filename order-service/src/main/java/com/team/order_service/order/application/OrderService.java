package com.team.order_service.order.application;

import com.team.order_service.order.application.dto.OrderCreateCommand;
import com.team.order_service.order.application.dto.OrderGetQuery;
import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.application.dto.OrderUpdateCommand;
import com.team.order_service.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResult createOrder(OrderCreateCommand command);

    OrderResult getOrder(UUID orderId);

    Page<OrderResult> getOrders(OrderGetQuery query, Pageable pageable);

    OrderResult updateOrder(OrderUpdateCommand command);

    void updateOrderStatus(UUID orderId, OrderStatus status);

    void cancelOrder(UUID orderId, UUID cancelledBy);
}
