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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
            ProductResponse product = getProductWithFallback(command.orderedBy(), orderItemCommand.productId());
            OrderItem orderItem = OrderItem.create(
                saved,
                orderItemCommand.productId(),
                product.name(),
                product.unitPrice(),
                orderItemCommand.quantity()
            );
            saved.addOrderItem(orderItem);
        });

        // 3. 재고 차감 (실패 시 이미 차감된 항목 복원 - 보상 트랜잭션)
        // TODO: [사가 패턴 도입 시 개선 필요]
        //   - 현재 REST 기반 보상 트랜잭션은 멱등성 보장 불가
        //   - 보상 후 재시도 시 동일 orderId로 중복 차감 발생 가능
        //   - 사가 패턴 도입 시 이벤트 ID 기반 중복 처리 방지로 해결 예정
        int deductedCount = 0;
        var items = command.orderItems();
        try {
            for (var orderItemCommand : command.orderItems()) {
                deductStockWithFallback(
                    command.orderedBy(),
                    orderItemCommand.productId(),
                    orderItemCommand.quantity(),
                    saved.getId()
                );
                deductedCount++;
            }
        } catch (BusinessException e) {
            // 이미 차감된 항목 복원
            for (int i = 0; i < deductedCount; i++) {
                try {
                    productClient.restoreStock(
                        command.orderedBy(),
                        items.get(i).productId(),
                        new StockRestoreRequest(items.get(i).quantity(), saved.getId())
                    );
                } catch (Exception ignored) {
                }
            }
            throw e;
        }


//        // 4. 배송 생성 (TODO: 배송 서비스 API 확정 후 주석 해제)
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

        // 1. 재고 복원 (실패 시 이미 복원된 항목 다시 차감 - 보상 트랜잭션)
        // TODO: [사가 패턴 도입 시 개선 필요]
        //   - 현재 REST 기반 보상 트랜잭션은 멱등성 보장 불가
        //   - 보상 후 재시도 시 동일 orderId로 중복 복원 발생 가능
        //   - 사가 패턴 도입 시 이벤트 ID 기반 중복 처리 방지로 해결 예정
        int restoredCount = 0;
        List<OrderItem> orderItems = order.getOrderItems();
        try {
            for (var orderItem : orderItems) {
                restoreStockWithFallback(
                    cancelledBy,
                    orderItem.getProductId(),
                    orderItem.getQuantity(),
                    orderId
                );
                restoredCount++;
            }
        } catch (BusinessException e) {
            // 이미 복원된 항목 다시 차감 (보상)
            for (int i = 0; i < restoredCount; i++) {
                try {
                    productClient.deductStock(
                        cancelledBy,
                        orderItems.get(i).getProductId(),
                        new StockDeductRequest(orderItems.get(i).getQuantity(), orderId)
                    );
                } catch (Exception ignored) {
                }
            }
            throw e;
        }

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

    @Override
    @Transactional
    public void deleteOrder(UUID orderId, UUID deletedBy) {
        Order order = findActiveOrderById(orderId);
        order.softDelete(deletedBy);
        deliveryClient.deleteDelivery(order.getDeliveryId());
    }

    // -------------------------------------------------------
    // private helpers - Feign 예외 타입별 분기 처리
    // -------------------------------------------------------

    private ProductResponse getProductWithFallback(UUID requestUserId, UUID productId) {
        try {
            return productClient.getProduct(requestUserId, productId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        } catch (Exception e) {
            throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void deductStockWithFallback(UUID requestUserId, UUID productId, int quantity, UUID orderId) {
        try {
            productClient.deductStock(requestUserId, productId, new StockDeductRequest(quantity, orderId));
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        } catch (FeignException e) {
            throw new BusinessException(OrderErrorCode.STOCK_DEDUCT_FAILED);
        } catch (Exception e) {
            throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void restoreStockWithFallback(UUID requestUserId, UUID productId, int quantity, UUID orderId) {
        try {
            productClient.restoreStock(requestUserId, productId, new StockRestoreRequest(quantity, orderId));
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        } catch (FeignException e) {
            throw new BusinessException(OrderErrorCode.STOCK_RESTORE_FAILED);
        } catch (Exception e) {
            throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private Order findActiveOrderById(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
