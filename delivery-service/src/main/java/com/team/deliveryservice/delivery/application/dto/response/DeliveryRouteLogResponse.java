package com.team.deliveryservice.delivery.application.dto.response;

import com.team.deliveryservice.delivery.domain.DeliveryRouteLog;
import com.team.deliveryservice.delivery.domain.DeliveryRouteStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeliveryRouteLogResponse(
    UUID routeLogId,
    Integer sequenceNo,
    UUID departureHubId,
    UUID arrivalHubId,
    BigDecimal expectedDistanceKm,
    Integer expectedDurationMinutes,
    Integer realDurationMinutes,
    DeliveryRouteStatus routeStatus,
    UUID deliveryManagerId,
    LocalDateTime departedAt,
    LocalDateTime arrivedAt
) {
    public static DeliveryRouteLogResponse from(DeliveryRouteLog routeLog) {
        return DeliveryRouteLogResponse.builder()
            .routeLogId(routeLog.getId())
            .sequenceNo(routeLog.getSequenceNo())
            .departureHubId(routeLog.getDepartureHubId())
            .arrivalHubId(routeLog.getArrivalHubId())
            .expectedDistanceKm(routeLog.getExpectedDistanceKm())
            .expectedDurationMinutes(routeLog.getExpectedDurationMinutes())
            .realDurationMinutes(routeLog.getRealDurationMinutes())
            .routeStatus(routeLog.getRouteStatus())
            .deliveryManagerId(routeLog.getDeliveryManagerId())
            .departedAt(routeLog.getDepartedAt())
            .arrivedAt(routeLog.getArrivedAt())
            .build();
    }
}
