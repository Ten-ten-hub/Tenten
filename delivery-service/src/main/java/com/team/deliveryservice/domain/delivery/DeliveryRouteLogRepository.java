package com.team.deliveryservice.domain.delivery;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRouteLogRepository extends JpaRepository<DeliveryRouteLog, UUID> {

    List<DeliveryRouteLog> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);
}
