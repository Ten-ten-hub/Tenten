package com.team.order_service.order.domain;

public enum OrderStatus {
    CREATED,            // 주문 생성
    CONFIRMED,          // 주문 확인
    READY_FOR_DELIVERY, // 배송 준비 완료
    IN_DELIVERY,        // 배송 중
    CANCELLED,          // 주문 취소
    COMPLETED           // 주문 완료
}
