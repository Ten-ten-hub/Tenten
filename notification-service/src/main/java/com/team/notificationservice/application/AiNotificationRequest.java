package com.team.notificationservice.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiNotificationRequest(
    UUID orderId,
    UUID receiverId,           // 수신자 UUID
    String receiverSlackId,      // 발송 허브 담당자 슬랙 ID
    String msgContent,      // AI가 생성한 최종 배송 가이드 문구
    LocalDateTime scheduledAt, // AI가 계산한 발송 예정 시각
    UUID refId
) {
}
