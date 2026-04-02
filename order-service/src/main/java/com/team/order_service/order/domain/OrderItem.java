package com.team.order_service.order.domain;

import com.team.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "p_order_item")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name_snapshot", nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public static OrderItem create(Order order, UUID productId,
                                   String productNameSnapshot, BigDecimal unitPriceSnapshot,
                                   int quantity) {
        return OrderItem.builder()
            .order(order)
            .productId(productId)
            .productNameSnapshot(productNameSnapshot)
            .unitPriceSnapshot(unitPriceSnapshot)
            .quantity(quantity)
            .build();
    }
}
