package com.team.order_service.global.exception;

import com.team.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum OrderErrorCode implements ErrorCode {

    // 404
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
