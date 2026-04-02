package com.team.product_service.stock.domain;

import com.team.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_stock_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "stock_history_type")
    private StockHistoryType type;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "order_id")
    private UUID orderId;

    @Builder
    private StockHistory(UUID stockId, StockHistoryType type,
                         Integer quantityChange, Integer quantityAfter, UUID orderId) {
        this.stockId = stockId;
        this.type = type;
        this.quantityChange = quantityChange;
        this.quantityAfter = quantityAfter;
        this.orderId = orderId;
    }

    public static StockHistory of(UUID stockId, StockHistoryType type, int quantityChange, int quantityAfter, UUID orderId) {
        return
            StockHistory.builder()
                .stockId(stockId)
                .type(type)
                .quantityChange(quantityChange)
                .quantityAfter(quantityAfter)
                .orderId(orderId)
                .build();
    }
}
