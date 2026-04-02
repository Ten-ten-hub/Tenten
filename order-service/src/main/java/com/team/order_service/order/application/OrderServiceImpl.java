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
import com.team.order_service.order.infrastructure.client.DeliveryClient;
import com.team.order_service.order.infrastructure.client.ProductClient;
import com.team.order_service.order.infrastructure.client.dto.ProductResponse;
import com.team.order_service.order.infrastructure.client.dto.StockDeductRequest;
import com.team.order_service.order.infrastructure.client.dto.StockRestoreRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final DeliveryClient deliveryClient;

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

        // 2. 주문 아이템 추가
        command.orderItems().forEach(orderItemCommand -> {
            ProductResponse product;
            try {
                product = productClient.getProduct(command.orderedBy(), orderItemCommand.productId());
            } catch (Exception e) {
                throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
            }
            OrderItem orderItem = OrderItem.create(
                saved,
                orderItemCommand.productId(),
                product.name(),
                product.unitPrice(),
                orderItemCommand.quantity()
            );
            saved.addOrderItem(orderItem);
        });

        // 3. 재고 차감
        command.orderItems().forEach(orderItemCommand -> {
            try {
                productClient.deductStock(
                    command.orderedBy(),
                    orderItemCommand.productId(),
                    new StockDeductRequest(orderItemCommand.quantity(), saved.getId())
                );
            } catch (Exception e) {
                throw new BusinessException(OrderErrorCode.STOCK_DEDUCT_FAILED);
            }
        });

//        // 4. 배송 생성 (배송 연동 후 주석 해제)
//        DeliveryResponse delivery;
//        try {
//            delivery = deliveryClient.createDelivery(
//                command.orderedBy(),
//                new DeliveryCreateRequest(saved.getId(), command.supplierCompanyId(), command.receiverCompanyId())
//            );
//        } catch (Exception e) {
//            throw new BusinessException(OrderErrorCode.DELIVERY_CREATE_FAILED);
//        }
//
//        saved.assignDelivery(delivery.id());

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

        // 1. 재고 복원
        order.getOrderItems().forEach(orderItem -> {
            try {
                productClient.restoreStock(
                    cancelledBy,
                    orderItem.getProductId(),
                    new StockRestoreRequest(orderItem.getQuantity(), orderId)
                );
            } catch (Exception e) {
                throw new BusinessException(OrderErrorCode.STOCK_RESTORE_FAILED);
            }
        });

//        // 2. 배송 취소 (배송 연동 후 주석 해제)
//        if (order.getDeliveryId() != null) {
//            try {
//                deliveryClient.cancelDelivery(cancelledBy, order.getDeliveryId());
//            } catch (Exception e) {
//                throw new BusinessException(OrderErrorCode.DELIVERY_CANCEL_FAILED);
//            }
//        }

        // 3. 주문 취소
        order.cancel(cancelledBy);
    }

    private Order findActiveOrderById(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
