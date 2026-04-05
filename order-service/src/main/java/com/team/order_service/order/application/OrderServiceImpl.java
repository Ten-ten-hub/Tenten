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
import com.team.order_service.order.infrastructure.client.dto.*;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
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

        // 3. 재고 차감 + 배송 생성 (실패 시 차감된 재고 전체 복원 - 보상 트랜잭션)
        // TODO: [사가 패턴 도입 시 개선 필요]
        //   - 현재 REST 기반 보상 트랜잭션은 멱등성 보장 불가
        //   - 보상 후 재시도 시 동일 orderId로 중복 차감 발생 가능
        //   - 사가 패턴 도입 시 이벤트 ID 기반 중복 처리 방지로 해결 예정
        int deductedCount = 0;
        var items = command.orderItems();
        try {
            for (var orderItemCommand : items) {
                deductStockWithFallback(
                        command.orderedBy(),
                        orderItemCommand.productId(),
                        orderItemCommand.quantity(),
                        saved.getId()
                );
                deductedCount++;
            }

            // 4. 배송 생성 (재고 차감 성공 후 시도)
            UUID deliveryId = createDeliveryWithFallback(command, saved.getId());
            saved.assignDelivery(deliveryId);

        } catch (Exception e) {
            // 차감된 재고 전체 복원
            for (int i = 0; i < deductedCount; i++) {
                try {
                    productClient.restoreStock(
                            command.orderedBy(),
                            items.get(i).productId(),
                            new StockRestoreRequest(items.get(i).quantity(), saved.getId())
                    );
                } catch (Exception compensationEx) {
                    log.error("[주문생성 보상 트랜잭션 실패] 재고 복원 실패 orderId={}, productId={}",
                            saved.getId(), items.get(i).productId(), compensationEx);
                }
            }
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException(OrderErrorCode.STOCK_DEDUCT_FAILED);
        }

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

        // 1. 재고 복원 + 배송 취소 (실패 시 복원된 재고 다시 차감 - 보상 트랜잭션)
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

            // 2. 배송 취소 (재고 복원 성공 후 시도)
            if (order.getDeliveryId() != null) {
                cancelDeliveryWithFallback(order.getDeliveryId());
            }

        } catch (Exception e) {
            // 복원된 재고 다시 차감 (보상)
            for (int i = 0; i < restoredCount; i++) {
                try {
                    productClient.deductStock(
                            cancelledBy,
                            orderItems.get(i).getProductId(),
                            new StockDeductRequest(orderItems.get(i).getQuantity(), orderId)
                    );
                } catch (Exception compensationEx) {
                    log.error("[주문취소 보상 트랜잭션 실패] 재고 재차감 실패 orderId={}, productId={}",
                            orderId, orderItems.get(i).getProductId(), compensationEx);
                }
            }
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException(OrderErrorCode.STOCK_RESTORE_FAILED);
        }

        // 3. 주문 취소
        order.cancel(cancelledBy);
    }

    @Override
    @Transactional
    public void deleteOrder(UUID orderId, UUID deletedBy) {
        Order order = findActiveOrderById(orderId);

        // 취소된 주문만 삭제 가능
        if (order.getOrderStatus() != OrderStatus.CANCELLED
                && order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_DELETABLE);
        }
        
        // 배송 삭제
        if (order.getDeliveryId() != null) {
            deleteDeliveryWithFallback(order.getDeliveryId());
        }

        order.softDelete(deletedBy);
    }

    // -------------------------------------------------------
    // private helpers - Feign 예외 타입별 분기 처리
    // -------------------------------------------------------

    private ProductResponse getProductWithFallback(UUID requestUserId, UUID productId) {
        try {
            return productClient.getProduct(requestUserId, productId);
        } catch (FeignException.NotFound e) {
            log.warn("[상품 조회] 상품 없음 productId={}", productId, e);
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        } catch (Exception e) {
            log.error("[상품 조회 실패] productId={}", productId, e);
            throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void deductStockWithFallback(UUID requestUserId, UUID productId, int quantity, UUID orderId) {
        try {
            productClient.deductStock(requestUserId, productId, new StockDeductRequest(quantity, orderId));
        } catch (FeignException.NotFound e) {
            log.warn("[재고 차감] 상품 없음 productId={}", productId, e);
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        } catch (FeignException e) {
            log.error("[재고 차감 실패] productId={}, status={}", productId, e.status(), e);
            if (e.status() >= 500) {
                throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
            }
            throw new BusinessException(OrderErrorCode.STOCK_DEDUCT_FAILED);
        } catch (Exception e) {
            log.error("[재고 차감 실패] productId={}", productId, e);
            throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void restoreStockWithFallback(UUID requestUserId, UUID productId, int quantity, UUID orderId) {
        try {
            productClient.restoreStock(requestUserId, productId, new StockRestoreRequest(quantity, orderId));
        } catch (FeignException.NotFound e) {
            log.warn("[재고 복원] 상품 없음 productId={}", productId, e);
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
        } catch (FeignException e) {
            log.error("[재고 복원 실패] productId={}, status={}", productId, e.status(), e);
            if (e.status() >= 500) {
                throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
            }
            throw new BusinessException(OrderErrorCode.STOCK_RESTORE_FAILED);
        } catch (Exception e) {
            log.error("[재고 복원 실패] productId={}", productId, e);
            throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private UUID createDeliveryWithFallback(OrderCreateCommand command, UUID orderId) {
        try {
            DeliveryApiResponse response = deliveryClient.createDelivery(
                    new DeliveryCreateRequest(
                            orderId,
                            command.orderedBy(),
                            command.supplierCompanyId(),
                            command.receiverCompanyId(),
                            command.deadlineAt(),
                            command.requestNote()
                    )
            );
            if (!response.success() || response.data() == null || response.data().deliveryId() == null) {
                if (response.data() == null) {
                    log.error("[배송 생성 실패] 응답 data가 null입니다 orderId={}, code={}, message={}",
                            orderId, response.code(), response.message());
                } else {
                    log.error("[배송 생성 실패] deliveryId가 null입니다 orderId={}, code={}, message={}",
                            orderId, response.code(), response.message());
                }
                throw new BusinessException(OrderErrorCode.DELIVERY_CREATE_FAILED);
            }
            return response.data().deliveryId();
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            log.error("[배송 생성 실패] 404 orderId={}", orderId, e);
            throw new BusinessException(OrderErrorCode.DELIVERY_CREATE_FAILED);
        } catch (Exception e) {
            log.error("[배송 생성 실패] orderId={}", orderId, e);
            throw new BusinessException(OrderErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void cancelDeliveryWithFallback(UUID deliveryId) {
        try {
            deliveryClient.cancelDelivery(deliveryId);
        } catch (FeignException.NotFound e) {
            log.warn("[배송 취소] 이미 취소된 배송 deliveryId={}", deliveryId, e);
            // 이미 취소된 배송은 무시
        } catch (Exception e) {
            log.error("[배송 취소 실패] deliveryId={}", deliveryId, e);
            throw new BusinessException(OrderErrorCode.DELIVERY_CANCEL_FAILED);
        }
    }

    private void deleteDeliveryWithFallback(UUID deliveryId) {
        try {
            deliveryClient.deleteDelivery(deliveryId);
        } catch (FeignException.NotFound e) {
            log.warn("[배송 삭제] 이미 삭제된 배송 deliveryId={}", deliveryId, e);
            // 이미 삭제된 배송은 무시
        } catch (Exception e) {
            log.error("[배송 삭제 실패] deliveryId={}", deliveryId, e);
            throw new BusinessException(OrderErrorCode.DELIVERY_DELETE_FAILED);
        }
    }

    private Order findActiveOrderById(UUID orderId) {
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
