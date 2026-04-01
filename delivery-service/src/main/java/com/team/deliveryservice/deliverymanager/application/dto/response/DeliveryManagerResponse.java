package com.team.deliveryservice.deliverymanager.application.dto.response;

import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryManagerResponse(
    UUID deliveryManagerId,
    UUID hubId,
    String slackId,
    DeliveryManagerType type,
    Integer deliverySequence,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static DeliveryManagerResponse from(DeliveryManager deliveryManager) {
        return DeliveryManagerResponse.builder()
            .deliveryManagerId(deliveryManager.getId())
            .hubId(deliveryManager.getHubId())
            .slackId(deliveryManager.getSlackId())
            .type(deliveryManager.getType())
            .deliverySequence(deliveryManager.getDeliverySequence())
            .createdAt(deliveryManager.getCreatedAt())
            .updatedAt(deliveryManager.getUpdatedAt())
            .build();
    }
}
