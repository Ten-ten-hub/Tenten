package com.team.order_service.global.exception;

import com.team.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum OrderErrorCode implements ErrorCode {

    //400
    ORDER_NOT_CANCELLABLE("ORDER_NOT_CANCELLABLE", "취소할 수 없는 주문 상태입니다", HttpStatus.BAD_REQUEST),
    ORDER_STATUS_NOT_UPDATABLE("ORDER_STATUS_NOT_UPDATABLE", "변경할 수 없는 주문 상태입니다", HttpStatus.BAD_REQUEST),
    // 404
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다", HttpStatus.NOT_FOUND),


    // 500
    STOCK_DEDUCT_FAILED("STOCK_DEDUCT_FAILED", "재고 차감에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    STOCK_RESTORE_FAILED("STOCK_RESTORE_FAILED", "재고 복원에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    DELIVERY_CREATE_FAILED("DELIVERY_CREATE_FAILED", "배송 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    DELIVERY_CANCEL_FAILED("DELIVERY_CANCEL_FAILED", "배송 취소에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR);


    private final String code;
    private final String message;
    private final HttpStatus status;
}
