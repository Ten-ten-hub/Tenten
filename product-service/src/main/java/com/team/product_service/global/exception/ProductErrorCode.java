package com.team.product_service.global.exception;

import com.team.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DUPLICATE_PRODUCT("DUPLICATE_PRODUCT", "이미 존재하는 상품입니다", HttpStatus.CONFLICT),
    PRODUCT_DELETED("PRODUCT_DELETED", "삭제된 상품입니다", HttpStatus.GONE);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
