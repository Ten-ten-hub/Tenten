package com.team.notificationservice.presentation.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통 에러
    COMMON_INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "입력값이 올바르지 않습니다."),
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // 알림 서비스 전용 에러
    NOTI_RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI_RECIPIENT_NOT_FOUND", "알림 수신 대상자(슬랙 ID 또는 이메일)를 찾을 수 없습니다."),
    NOTI_NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI_NOT_FOUND", "해당 알림 정보를 찾을 수 없습니다."),
    NOTI_SLACK_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "NOTI_SLACK_ERROR", "슬랙 API 호출 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
