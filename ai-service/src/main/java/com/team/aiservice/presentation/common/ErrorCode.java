package com.team.aiservice.presentation.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements com.team.common.exception.ErrorCode {
    AI_ANALYSIS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI_ERROR", "AI 분석 서비스 호출에 실패했습니다."),
    AI_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_NOT_FOUND", "해당 AI 분석 기록을 찾을 수 없습니다."),
    HUB_ROUTE_NOT_FOUND(HttpStatus.BAD_REQUEST, "HUB_ROUTE_NOT_FOUND", "유효한 허브 경로 정보를 가져올 수 없습니다."),
    COMMON_INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "입력값이 올바르지 않습니다."),
    COMMON_SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_SYSTEM_ERROR", "시스템 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
