package com.team.deliveryservice.domain.delivery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRouteLogRepository extends JpaRepository<DeliveryRouteLog, UUID> {

    Optional<DeliveryRouteLog> findByIdAndDeletedAtIsNull(UUID id);

    List<DeliveryRouteLog> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);

    List<DeliveryRouteLog> findAllByDeliveryIdInAndDeletedAtIsNullOrderBySequenceNoAsc(List<UUID> deliveryIds);
}
