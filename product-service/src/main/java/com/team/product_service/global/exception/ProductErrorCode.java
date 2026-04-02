package com.team.product_service.global.exception;

import com.team.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    // 400
    STOCK_NOT_ENOUGH("STOCK_NOT_ENOUGH", "재고가 부족합니다", HttpStatus.BAD_REQUEST),
    PRODUCT_ALREADY_DELETED("PRODUCT_ALREADY_DELETED", "이미 삭제된 상품입니다", HttpStatus.BAD_REQUEST),
    STOCK_BELOW_ZERO("STOCK_BELOW_ZERO", "재고는 0 미만이 될 수 없습니다", HttpStatus.BAD_REQUEST),
    PRODUCT_DISCONTINUED("PRODUCT_DISCONTINUED", "판매 중단된 상품입니다", HttpStatus.BAD_REQUEST),
    INVALID_STOCK_AMOUNT("INVALID_STOCK_AMOUNT", "유효하지 않은 재고 변경값입니다", HttpStatus.BAD_REQUEST),

    // 404
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    STOCK_NOT_FOUND("STOCK_NOT_FOUND", "재고를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // 409
    DUPLICATE_PRODUCT_NAME("DUPLICATE_PRODUCT_NAME", "이미 존재하는 상품명입니다", HttpStatus.CONFLICT),

    // 500
    STOCK_RESTORE_FAILED("STOCK_RESTORE_FAILED", "재고 복원에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR);


    private final String code;
    private final String message;
    private final HttpStatus status;
}
