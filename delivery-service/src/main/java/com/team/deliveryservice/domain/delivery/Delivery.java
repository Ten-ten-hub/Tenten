package com.team.deliveryservice.domain.delivery;

import com.team.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_delivery")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "delivery_status")
    private DeliveryStatus deliveryStatus;

    @Column(nullable = false)
    private UUID originHubId;

    @Column(nullable = false)
    private UUID destinationHubId;

    @Column(nullable = false)
    private UUID receiverCompanyId;

    @Column(nullable = false, length = 255)
    private String deliveryAddress;

    @Column(length = 255)
    private String deliveryAddressDetail;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 100)
    private String recipientSlackId;

    @Column
    private UUID companyDeliveryManagerId;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime finalDispatchDeadlineAt;

    @Builder
    private Delivery(
        UUID id,
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
        LocalDateTime finalDispatchDeadlineAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.deliveryStatus = deliveryStatus;
        this.originHubId = originHubId;
        this.destinationHubId = destinationHubId;
        this.receiverCompanyId = receiverCompanyId;
        this.deliveryAddress = deliveryAddress;
        this.deliveryAddressDetail = deliveryAddressDetail;
        this.recipientName = recipientName;
        this.recipientSlackId = recipientSlackId;
        this.companyDeliveryManagerId = companyDeliveryManagerId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.finalDispatchDeadlineAt = finalDispatchDeadlineAt;
    }

    public static Delivery create(
        UUID orderId,
        UUID originHubId,
        UUID destinationHubId,
        UUID receiverCompanyId,
        String deliveryAddress,
        String deliveryAddressDetail,
        String recipientName,
        String recipientSlackId,
        UUID companyDeliveryManagerId,
        LocalDateTime finalDispatchDeadlineAt
    ) {
        return Delivery.builder()
            .id(UUID.randomUUID())
            .orderId(orderId)
            .deliveryStatus(DeliveryStatus.WAITING_AT_HUB)
            .originHubId(originHubId)
            .destinationHubId(destinationHubId)
            .receiverCompanyId(receiverCompanyId)
            .deliveryAddress(deliveryAddress)
            .deliveryAddressDetail(deliveryAddressDetail)
            .recipientName(recipientName)
            .recipientSlackId(recipientSlackId)
            .companyDeliveryManagerId(companyDeliveryManagerId)
            .finalDispatchDeadlineAt(finalDispatchDeadlineAt)
            .build();
    }

    public void updateInfo(
        String deliveryAddress,
        String deliveryAddressDetail,
        String recipientName,
        String recipientSlackId,
        UUID companyDeliveryManagerId
    ) {
        this.deliveryAddress = deliveryAddress;
        this.deliveryAddressDetail = deliveryAddressDetail;
        this.recipientName = recipientName;
        this.recipientSlackId = recipientSlackId;
        this.companyDeliveryManagerId = companyDeliveryManagerId;
    }

    public void updateStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;

        if (this.startedAt == null && deliveryStatus == DeliveryStatus.MOVING_BETWEEN_HUBS) {
            this.startedAt = LocalDateTime.now();
        }

        // 최초 완료 시각만 기록하도록 수정
        if (this.completedAt == null && deliveryStatus == DeliveryStatus.DELIVERED) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
