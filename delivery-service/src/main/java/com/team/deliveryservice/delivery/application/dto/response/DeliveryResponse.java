package com.team.deliveryservice.delivery.application.dto.response;

import com.team.deliveryservice.delivery.domain.Delivery;
import com.team.deliveryservice.delivery.domain.DeliveryStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryResponse(
    UUID deliveryId,
    UUID orderId,
    DeliveryStatus deliveryStatus,
    UUID originHubId,
    UUID destinationHubId,
    UUID receiverCompanyId,
    String deliveryAddress,
    String deliveryAddressDetail,
    String recipientName,
    String recipientSlackId,
    UUID companyDeliveryManagerId,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime finalDispatchDeadlineAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<DeliveryRouteLogResponse> routeLogs
) {
    public static DeliveryResponse from(Delivery delivery, List<DeliveryRouteLogResponse> routeLogs) {
        return DeliveryResponse.builder()
            .deliveryId(delivery.getId())
            .orderId(delivery.getOrderId())
            .deliveryStatus(delivery.getDeliveryStatus())
            .originHubId(delivery.getOriginHubId())
            .destinationHubId(delivery.getDestinationHubId())
            .receiverCompanyId(delivery.getReceiverCompanyId())
            .deliveryAddress(delivery.getDeliveryAddress())
            .deliveryAddressDetail(delivery.getDeliveryAddressDetail())
            .recipientName(delivery.getRecipientName())
            .recipientSlackId(delivery.getRecipientSlackId())
            .companyDeliveryManagerId(delivery.getCompanyDeliveryManagerId())
            .startedAt(delivery.getStartedAt())
            .completedAt(delivery.getCompletedAt())
            .finalDispatchDeadlineAt(delivery.getFinalDispatchDeadlineAt())
            .createdAt(delivery.getCreatedAt())
            .updatedAt(delivery.getUpdatedAt())
            .routeLogs(routeLogs)
            .build();
    }
}
