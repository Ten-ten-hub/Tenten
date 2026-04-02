package com.team.order_service.order.application;

import com.team.common.exception.BusinessException;
import com.team.order_service.global.exception.OrderErrorCode;
import com.team.order_service.order.application.dto.OrderCreateCommand;
import com.team.order_service.order.application.dto.OrderGetQuery;
import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.application.dto.OrderUpdateCommand;
import com.team.order_service.order.domain.Order;
import com.team.order_service.order.domain.OrderItem;
import com.team.order_service.order.domain.OrderRepository;
import com.team.order_service.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResult createOrder(OrderCreateCommand command) {

        // 1. 주문 생성
        Order order = Order.create(
            command.orderedBy(),
            command.supplierCompanyId(),
            command.receiverCompanyId(),
            command.deadlineAt(),
            command.requestNote()
        );
        Order saved = orderRepository.save(order);

        // 2. 주문 아이템 추가 (TODO: Feign Client 연동 후 실제 상품 정보로 교체)
        command.orderItems().forEach(itemCommand -> {
            OrderItem orderItem = OrderItem.create(
                saved,
                itemCommand.productId(),
                "상품명 임시",               // TODO: productClient.getProduct()로 교체
                BigDecimal.ZERO,             // TODO: 실제 단가로 교체
                itemCommand.quantity()
            );
            saved.addOrderItem(orderItem);
        });

        // TODO: 재고 차감 (Feign Client 연동 후 추가)

        // TODO: 배송 생성 및 배송 ID 저장 (Feign Client 연동 후 추가)

        return OrderResult.from(saved);
    }

    @Override
    public OrderResult getOrder(UUID orderId) {
        Order order = findActiveOrderById(orderId);
        return OrderResult.from(order);
    }

    @Override
    public Page<OrderResult> getOrders(OrderGetQuery query, Pageable pageable) {
        return orderRepository.search(
            query.orderedBy(),
            query.supplierCompanyId(),
            query.receiverCompanyId(),
            query.orderStatus(),
            pageable
        ).map(OrderResult::from);
    }

    @Override
    @Transactional
    public OrderResult updateOrder(OrderUpdateCommand command) {
        Order order = findActiveOrderById(command.orderId());
        order.update(command.deadlineAt(), command.requestNote());

        return OrderResult.from(order);
    }

    @Override
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = findActiveOrderById(orderId);
        order.updateStatus(status);
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId, UUID cancelledBy) {
        Order order = findActiveOrderById(orderId);

        if (!order.isCancellable()) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
        }

        // TODO: 재고 복원 (Feign Client 연동 후 추가)

        // TODO: 배송 취소 (Feign Client 연동 후 추가)

        // 3. 주문 취소
        order.cancel(cancelledBy);
    }

    private Order findActiveOrderById(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
