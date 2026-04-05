package com.team.notificationservice.domain;

public enum SendStatus {
    PENDING,            // 대기 중
    SENT_IMMEDIATELY,   // 생성 직후 즉시 발송 완료 (추가)
    RESENT_DAILY,       // 당일 아침 재발송 완료 (추가)
    SUCCESS,
    FAIL
}
