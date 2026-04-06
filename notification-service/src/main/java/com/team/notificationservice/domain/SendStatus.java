package com.team.notificationservice.domain;

import lombok.Getter;

@Getter
public enum SendStatus {
    PENDING("대기 중"),
    SENT_IMMEDIATELY("즉시 발송 완료"),
    RESENT_DAILY("당일 재발송 완료"),
    SUCCESS("성공"),
    FAIL("실패");

    private final String description;

    SendStatus(String description) {
        this.description = description;
    }

    // 성공/전송 완료로 간주하는 헬퍼 메서드 추가
    public boolean isDelivered() {
        return this == SUCCESS || this == SENT_IMMEDIATELY || this == RESENT_DAILY;
    }
}
