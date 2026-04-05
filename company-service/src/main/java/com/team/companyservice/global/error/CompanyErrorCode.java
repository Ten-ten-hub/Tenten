package com.team.companyservice.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CompanyErrorCode implements com.team.common.exception.ErrorCode {

    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "잘못된 요청입니다."),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_UNAUTHORIZED", "인증이 필요합니다."),
    COMMON_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_ACCESS_DENIED", "접근 권한이 없습니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR", "서버 내부 오류입니다."),

    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "업체를 찾을 수 없습니다."),
    COMPANY_DUPLICATED(HttpStatus.CONFLICT, "COMPANY_DUPLICATED", "같은 허브 내 동일한 업체명이 이미 존재합니다."),
    HUB_NOT_FOUND(HttpStatus.BAD_REQUEST, "HUB_NOT_FOUND", "존재하지 않는 허브입니다."),
    HUB_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "HUB_SERVICE_UNAVAILABLE", "허브 서비스 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
