package com.team.aiservice.application.service;

import com.team.aiservice.infrastructure.client.NotificationClient;
import java.util.UUID;

public record NotificationPublishedEvent(
    UUID analysisId,
    NotificationClient.AiNotificationRequest payload
) {
}
