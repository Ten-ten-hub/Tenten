package com.team.order_service.order.domain;

import com.team.common.BaseEntity;
import com.team.common.exception.BusinessException;
import com.team.order_service.global.exception.OrderErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_order")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ordered_by", nullable = false)
    private UUID orderedBy;

    @Column(name = "supplier_company_id", nullable = false)
    private UUID supplierCompanyId;

    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    @Column(name = "delivery_id", unique = true)
    private UUID deliveryId;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "request_note", length = 500)
    private String requestNote;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "order_status", nullable = false, columnDefinition = "order_status", length = 30)
    private OrderStatus orderStatus = OrderStatus.CREATED;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public static Order create(UUID orderedBy, UUID supplierCompanyId, UUID receiverCompanyId,
                               LocalDateTime deadlineAt, String requestNote) {
        return Order.builder()
            .orderedBy(orderedBy)
            .supplierCompanyId(supplierCompanyId)
            .receiverCompanyId(receiverCompanyId)
            .deadlineAt(deadlineAt)
            .requestNote(requestNote)
            .totalPrice(BigDecimal.ZERO)
            .orderStatus(OrderStatus.CREATED)
            .build();
    }

    @Override
    public void softDelete(UUID deletedBy) {
        super.softDelete(deletedBy);
        this.orderStatus = OrderStatus.DELETED;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        recalculateTotalPrice();
    }

    public void assignDelivery(UUID deliveryId) {
        this.deliveryId = deliveryId;
    }

    public void cancel(UUID cancelledBy) {
        this.orderStatus = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = cancelledBy;
    }

    public void update(LocalDateTime deadlineAt, String requestNote) {
        if (deadlineAt != null) this.deadlineAt = deadlineAt;
        if (requestNote != null) this.requestNote = requestNote;
    }

    public void updateStatus(OrderStatus status) {
        if (this.orderStatus == OrderStatus.CANCELLED || this.orderStatus == OrderStatus.COMPLETED) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_NOT_UPDATABLE);
        }
        this.orderStatus = status;
    }

    private void recalculateTotalPrice() {
        this.totalPrice = orderItems.stream()
            .map(item -> item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isCancellable() {
        return this.orderStatus == OrderStatus.CREATED
            || this.orderStatus == OrderStatus.CONFIRMED;
    }
}
