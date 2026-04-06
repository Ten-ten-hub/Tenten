package com.team.notificationservice.application;

import java.util.UUID;

/**
 * DB 저장 완료 후 슬랙 발송을 위해 던지는 이벤트 객체
 */
public record NotificationSavedEvent(
    UUID notificationId,
    String receiverSlackId,
    String msgContent
) {
}
