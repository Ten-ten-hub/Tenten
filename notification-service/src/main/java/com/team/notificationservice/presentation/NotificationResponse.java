package com.team.notificationservice.presentation;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.SendStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record NotificationResponse(
    UUID id,
    String message,
    SendStatus status,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .message(notification.getMsgContent())
            .status(notification.getSendStatus())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
