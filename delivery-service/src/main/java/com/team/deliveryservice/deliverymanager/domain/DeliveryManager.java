package com.team.deliveryservice.deliverymanager.domain;

import com.team.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_delivery_manager")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManager extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "hub_id")
    private UUID hubId;

    @Column(name = "slack_id", nullable = false, length = 100)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_manager_type", nullable = false, length = 50)
    private DeliveryManagerType type;

    @Column(name = "delivery_sequence", nullable = false)
    private Integer deliverySequence;

    @Builder
    private DeliveryManager(
        UUID id,
        UUID hubId,
        String slackId,
        DeliveryManagerType type,
        Integer deliverySequence
    ) {
        this.id = id;
        this.hubId = hubId;
        this.slackId = slackId;
        this.type = type;
        this.deliverySequence = deliverySequence;
    }

    public static DeliveryManager create(
        UUID id,
        UUID hubId,
        String slackId,
        DeliveryManagerType type,
        Integer deliverySequence
    ) {
        validate(type, hubId);

        return DeliveryManager.builder()
            .id(id)
            .hubId(hubId)
            .slackId(slackId)
            .type(type)
            .deliverySequence(deliverySequence)
            .build();
    }

    public void update(UUID hubId, String slackId, DeliveryManagerType type) {
        validate(type, hubId);
        this.hubId = hubId;
        this.slackId = slackId;
        this.type = type;
    }

    private static void validate(DeliveryManagerType type, UUID hubId) {
        if (type == null) {
            throw new IllegalArgumentException("배송 담당자 타입은 필수입니다.");
        }

        if (type == DeliveryManagerType.COMPANY_DELIVERY_MANAGER && hubId == null) {
            throw new IllegalArgumentException("업체 배송 담당자는 소속 허브 ID가 필요합니다.");
        }

        if (type == DeliveryManagerType.HUB_DELIVERY_MANAGER && hubId != null) {
            throw new IllegalArgumentException("허브 배송 담당자는 소속 허브 ID를 가지면 안 됩니다.");
        }
    }
}
