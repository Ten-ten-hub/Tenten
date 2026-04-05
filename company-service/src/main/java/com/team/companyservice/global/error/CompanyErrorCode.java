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
    HUB_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "HUB_SERVICE_UNAVAILABLE", "허브 서비스 호출에 실패했습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    USER_NOT_APPROVED(HttpStatus.BAD_REQUEST, "USER_NOT_APPROVED", "가입 승인된 사용자만 업체 관리자로 지정할 수 있습니다."),
    USER_ALREADY_AFFILIATED(HttpStatus.CONFLICT, "USER_ALREADY_AFFILIATED", "이미 다른 소속이 배정된 사용자입니다."),
    USER_ROLE_NOT_ASSIGNABLE(HttpStatus.BAD_REQUEST, "USER_ROLE_NOT_ASSIGNABLE", "업체 관리자로 지정할 수 없는 권한의 사용자입니다."),
    USER_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "USER_SERVICE_UNAVAILABLE", "유저 서비스 호출에 실패했습니다."),
    COMPANY_MANAGER_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "COMPANY_MANAGER_ALREADY_ASSIGNED", "이미 업체 관리자가 지정된 업체입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
