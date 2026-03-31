package com.team.product_service.product.domain;

public enum ProductStatus {
    ON_SALE,        // 판매 중 (정상적으로 주문 가능)
    OUT_OF_STOCK,   // 품절 (일시적으로 재고 없음, 재입고 가능)
    DISCONTINUED    // 판매 중단 (더 이상 판매하지 않는 상품)
}
