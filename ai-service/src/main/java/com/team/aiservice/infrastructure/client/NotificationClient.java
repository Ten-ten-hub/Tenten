package com.team.aiservice.infrastructure.client;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/v1/notifications/internal/ai-slack")
    void sendWithAi(
        @RequestBody AiNotificationRequest request,
        @RequestParam("msgType") String msgType
    );

    record AiNotificationRequest(
        UUID orderId,
        UUID receiverId,
        String receiverSlackId,
        String msgContent,
        LocalDateTime scheduledAt,
        UUID refId
    ) {
    }
}
