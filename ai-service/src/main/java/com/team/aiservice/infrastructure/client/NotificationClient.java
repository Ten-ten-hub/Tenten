package com.team.aiservice.infrastructure.client;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/internal/v1/notifications/ai-slack/update")
    void updateSlackMessage(
        @RequestBody AiUpdateNotificationRequest request);

    record AiNotificationRequest(
        UUID orderId,
        UUID receiverId,
        String receiverSlackId,
        String msgContent,
        LocalDateTime scheduledAt,
        UUID refId,
        String msgType
    ) {
    }

    @Getter
    @NoArgsConstructor
    class AiNotificationResponse {
        private String slackTs;
    }

    // 수정 요청을 위한 record
    record AiUpdateNotificationRequest(
        String slackTs,
        String msgContent,
        String receiverSlackId
    ) {
    }
}
