package com.team.notificationservice.application;

import java.util.UUID;

public record NotificationCreatedEvent(
    UUID notificationId,
    String receiverSlackId,
    String message
) {
}
