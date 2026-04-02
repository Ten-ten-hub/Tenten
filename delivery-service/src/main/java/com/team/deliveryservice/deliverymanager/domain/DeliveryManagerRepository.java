package com.team.deliveryservice.deliverymanager.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryManagerRepository
    extends JpaRepository<DeliveryManager, UUID>, DeliveryManagerRepositoryCustom {

    Optional<DeliveryManager> findByIdAndDeletedAtIsNull(UUID id);

    Optional<DeliveryManager> findTopByTypeAndDeletedAtIsNullOrderByDeliverySequenceDesc(
        DeliveryManagerType type
    );

    Optional<DeliveryManager> findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(
        DeliveryManagerType type,
        UUID hubId
    );
}
