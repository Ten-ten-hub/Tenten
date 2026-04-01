package com.team.deliveryservice.domain.deliverymanager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    Optional<DeliveryManager> findByIdAndDeletedAtIsNull(UUID id);

    List<DeliveryManager> findAllByDeletedAtIsNull();

    Optional<DeliveryManager> findTopByTypeAndDeletedAtIsNullOrderByDeliverySequenceDesc(
        DeliveryManagerType type
    );

    Optional<DeliveryManager> findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(
        DeliveryManagerType type,
        UUID hubId
    );
}
