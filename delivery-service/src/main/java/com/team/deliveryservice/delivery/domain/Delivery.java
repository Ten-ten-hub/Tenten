package com.team.deliveryservice.delivery.domain;

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
        if (this.deliveryStatus == DeliveryStatus.DELIVERED || this.deliveryStatus == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("완료 또는 취소된 배송은 수정할 수 없습니다.");
        }

        this.deliveryAddress = deliveryAddress;
        this.deliveryAddressDetail = deliveryAddressDetail;
        this.recipientName = recipientName;
        this.recipientSlackId = recipientSlackId;
        this.companyDeliveryManagerId = companyDeliveryManagerId;
    }

    public void updateStatus(DeliveryStatus nextStatus) {
        validateStatusChange(nextStatus);
        this.deliveryStatus = nextStatus;

        if (this.startedAt == null && nextStatus == DeliveryStatus.MOVING_BETWEEN_HUBS) {
            this.startedAt = LocalDateTime.now();
        }

        if (this.completedAt == null && nextStatus == DeliveryStatus.DELIVERED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void cancel() {
        if (this.deliveryStatus != DeliveryStatus.WAITING_AT_HUB) {
            throw new IllegalStateException("배송 취소는 WAITING_AT_HUB 상태에서만 가능합니다.");
        }

        this.deliveryStatus = DeliveryStatus.CANCELLED;
    }

    public void assignCompanyDeliveryManager(UUID companyDeliveryManagerId) {
        if (this.deliveryStatus == DeliveryStatus.DELIVERED || this.deliveryStatus == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("완료 또는 취소된 배송에는 업체 배송 담당자를 배정할 수 없습니다.");
        }

        this.companyDeliveryManagerId = companyDeliveryManagerId;
    }

    private void validateStatusChange(DeliveryStatus nextStatus) {
        if (this.deliveryStatus == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("완료된 배송은 상태를 변경할 수 없습니다.");
        }

        if (this.deliveryStatus == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("취소된 배송은 상태를 변경할 수 없습니다.");
        }

        if (this.deliveryStatus == nextStatus) {
            return;
        }

        boolean valid = switch (this.deliveryStatus) {
            case WAITING_AT_HUB -> nextStatus == DeliveryStatus.MOVING_BETWEEN_HUBS;
            case MOVING_BETWEEN_HUBS -> nextStatus == DeliveryStatus.ARRIVED_AT_DESTINATION_HUB;
            case ARRIVED_AT_DESTINATION_HUB -> nextStatus == DeliveryStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> nextStatus == DeliveryStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalStateException("허용되지 않은 배송 상태 전이입니다.");
        }
    }
}
