package com.team.notificationservice.infrastructure;

public enum SlackSendResult {
    SUCCESS,            // 전송 성공
    RETRYABLE_FAILURE,  // 명확한 실패 (재시도 가능)
    UNKNOWN             // 타임아웃 등 결과 불분명 (중복 방지를 위해 락 유지 필요)
}
