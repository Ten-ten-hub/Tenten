package com.team.product_service.stock.domain;

import com.team.common.BaseEntity;
import com.team.common.exception.BusinessException;
import com.team.product_service.global.exception.ProductErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Entity
@Table(name = "p_stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "stock_status")
    private StockStatus status = StockStatus.SOLD_OUT;

    public static Stock create(UUID productId, UUID hubId) {
        return Stock.builder()
            .productId(productId)
            .hubId(hubId)
            .quantity(0)
            .status(StockStatus.SOLD_OUT)
            .build();
    }

    public void increase(int amount) {
        this.quantity += amount;
        updateStatus();
    }

    public void decrease(int amount) {
        if (this.quantity - amount < 0) {
            throw new BusinessException(ProductErrorCode.STOCK_NOT_ENOUGH);
        }
        this.quantity -= amount;
        updateStatus();
    }

    public void adjust(int quantity) {
        if (quantity < 0) {
            throw new BusinessException(ProductErrorCode.STOCK_BELOW_ZERO);
        }
        this.quantity = quantity;
        updateStatus();
    }

    private void updateStatus() {
        if (this.quantity == 0) {
            this.status = StockStatus.SOLD_OUT;
        } else if (this.quantity <= 10) {
            this.status = StockStatus.SHORTAGE;
        } else {
            this.status = StockStatus.AVAILABLE;
        }
    }
}
