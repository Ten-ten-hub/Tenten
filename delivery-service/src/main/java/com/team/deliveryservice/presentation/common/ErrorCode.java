package com.team.deliveryservice.presentation.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode implements com.team.common.exception.ErrorCode {

    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "배송 정보를 찾을 수 없습니다."),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "DELIVERY_ALREADY_EXISTS", "이미 배송이 생성된 주문입니다."),
    DELIVERY_ROUTE_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_ROUTE_LOG_NOT_FOUND", "배송 경로 로그를 찾을 수 없습니다."),
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "잘못된 요청입니다."),
    COMMON_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_ACCESS_DENIED", "접근 권한이 없습니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
