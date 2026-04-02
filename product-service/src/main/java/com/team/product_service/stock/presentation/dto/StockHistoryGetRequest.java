package com.team.product_service.stock.presentation.dto;

import com.team.product_service.stock.application.dto.StockHistoryGetQuery;
import com.team.product_service.stock.domain.StockHistoryType;

import java.util.UUID;

public record StockHistoryGetRequest(
    UUID stockId,
    UUID productId,
    UUID orderId,
    StockHistoryType type
) {
    public StockHistoryGetQuery toQuery() {
        return new StockHistoryGetQuery(stockId, productId, orderId, type);
    }
}
