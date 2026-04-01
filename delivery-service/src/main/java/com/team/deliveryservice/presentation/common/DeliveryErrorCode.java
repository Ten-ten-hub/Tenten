package com.team.deliveryservice.presentation.common;

import org.springframework.http.HttpStatus;

public enum DeliveryErrorCode implements com.team.common.exception.ErrorCode {

    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "배송 정보를 찾을 수 없습니다."),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "DELIVERY_ALREADY_EXISTS", "이미 배송이 생성된 주문입니다."),
    DELIVERY_ROUTE_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_ROUTE_LOG_NOT_FOUND", "배송 경로 로그를 찾을 수 없습니다."),
    DELIVERY_STATUS_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DELIVERY_STATUS_CHANGE_NOT_ALLOWED", "배송 상태를 변경할 수 없습니다."),
    DELIVERY_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DELIVERY_CANCEL_NOT_ALLOWED", "현재 상태에서는 배송을 취소할 수 없습니다."),
    DELIVERY_ASSIGN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DELIVERY_ASSIGN_NOT_ALLOWED", "현재 상태에서는 배송 담당자를 배정할 수 없습니다."),
    DELIVERY_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "DELIVERY_ALREADY_COMPLETED", "이미 배송 완료된 건입니다."),
    DELIVERY_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "DELIVERY_ALREADY_CANCELLED", "이미 배송 취소된 건입니다."),
    DELIVERY_MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_MANAGER_NOT_FOUND", "배송 담당자를 찾을 수 없습니다."),
    DELIVERY_MANAGER_TYPE_INVALID(HttpStatus.BAD_REQUEST, "DELIVERY_MANAGER_TYPE_INVALID", "허용되지 않은 배송 담당자 타입입니다."),
    DELIVERY_MANAGER_HUB_MISMATCH(HttpStatus.BAD_REQUEST, "DELIVERY_MANAGER_HUB_MISMATCH", "배송 담당자의 소속 허브가 일치하지 않습니다."),
    DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED", "허브 배송 담당자를 배정할 수 없습니다."),
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "잘못된 요청입니다."),
    COMMON_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_ACCESS_DENIED", "접근 권한이 없습니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryErrorCode(HttpStatus status, String code, String message) {
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
