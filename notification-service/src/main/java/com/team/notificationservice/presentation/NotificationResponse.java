package com.team.notificationservice.presentation;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.SendStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {
    private UUID id;
    private String message;
    private SendStatus status;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .message(notification.getMsgContent())
                .status(notification.getSendStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}