package com.team.deliveryservice.deliverymanager.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;

public interface DeliveryManagerRepository
    extends JpaRepository<DeliveryManager, UUID>, DeliveryManagerRepositoryCustom {

    // 활성 상태의 배송담당자 단건 조회
    Optional<DeliveryManager> findByIdAndDeletedAtIsNull(UUID id);

    // 허브 배송 담당자(HUB_DELIVERY_MANAGER) 마지막 순번 조회 + 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManager> findTopByTypeAndDeletedAtIsNullOrderByDeliverySequenceDesc(
        DeliveryManagerType type
    );

    // 업체 배송 담당자(COMPANY_DELIVERY_MANAGER) 마지막 순번 조회 + 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManager> findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(
        DeliveryManagerType type,
        UUID hubId
    );

    List<DeliveryManager> findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
        DeliveryManagerType type,
        UUID hubId
    );
}
