package com.team.deliveryservice.domain.delivery;

import com.team.common.BaseEntity;
import com.team.deliveryservice.presentation.common.ErrorCode;
import com.team.deliveryservice.presentation.common.ServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_delivery_route_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRouteLog extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private Integer sequenceNo;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private UUID arrivalHubId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedDistanceKm;

    @Column(nullable = false)
    private Integer expectedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "delivery_route_status")
    private DeliveryRouteStatus routeStatus;

    @Column
    private UUID deliveryManagerId;

    @Column
    private LocalDateTime departedAt;

    @Column
    private LocalDateTime arrivedAt;

    @Builder
    private DeliveryRouteLog(
        UUID id,
        UUID deliveryId,
        Integer sequenceNo,
        UUID departureHubId,
        UUID arrivalHubId,
        BigDecimal expectedDistanceKm,
        Integer expectedDurationMinutes,
        DeliveryRouteStatus routeStatus,
        UUID deliveryManagerId,
        LocalDateTime departedAt,
        LocalDateTime arrivedAt
    ) {
        this.id = id;
        this.deliveryId = deliveryId;
        this.sequenceNo = sequenceNo;
        this.departureHubId = departureHubId;
        this.arrivalHubId = arrivalHubId;
        this.expectedDistanceKm = expectedDistanceKm;
        this.expectedDurationMinutes = expectedDurationMinutes;
        this.routeStatus = routeStatus;
        this.deliveryManagerId = deliveryManagerId;
        this.departedAt = departedAt;
        this.arrivedAt = arrivedAt;
    }

    public static DeliveryRouteLog create(
        UUID deliveryId,
        Integer sequenceNo,
        UUID departureHubId,
        UUID arrivalHubId,
        BigDecimal expectedDistanceKm,
        Integer expectedDurationMinutes,
        UUID deliveryManagerId
    ) {
        validate(deliveryId, sequenceNo, departureHubId, arrivalHubId, expectedDistanceKm, expectedDurationMinutes);

        return DeliveryRouteLog.builder()
            .id(UUID.randomUUID())
            .deliveryId(deliveryId)
            .sequenceNo(sequenceNo)
            .departureHubId(departureHubId)
            .arrivalHubId(arrivalHubId)
            .expectedDistanceKm(expectedDistanceKm)
            .expectedDurationMinutes(expectedDurationMinutes)
            .routeStatus(DeliveryRouteStatus.WAITING_AT_HUB)
            .deliveryManagerId(deliveryManagerId)
            .build();
    }

    private static void validate(
        UUID deliveryId,
        Integer sequenceNo,
        UUID departureHubId,
        UUID arrivalHubId,
        BigDecimal expectedDistanceKm,
        Integer expectedDurationMinutes
    ) {
        if (deliveryId == null) {
            throw new ServiceException(ErrorCode.COMMON_INVALID_INPUT);
        }

        if (sequenceNo == null || sequenceNo <= 0) {
            throw new ServiceException(ErrorCode.COMMON_INVALID_INPUT);
        }

        if (departureHubId == null || arrivalHubId == null) {
            throw new ServiceException(ErrorCode.COMMON_INVALID_INPUT);
        }

        if (departureHubId.equals(arrivalHubId)) {
            throw new ServiceException(ErrorCode.COMMON_INVALID_INPUT);
        }

        if (expectedDistanceKm == null || expectedDistanceKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(ErrorCode.COMMON_INVALID_INPUT);
        }

        if (expectedDurationMinutes == null || expectedDurationMinutes < 0) {
            throw new ServiceException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }
}
