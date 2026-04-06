package com.team.deliveryservice.delivery.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRouteLogRepository extends JpaRepository<DeliveryRouteLog, UUID> {

    Optional<DeliveryRouteLog> findByIdAndDeletedAtIsNull(UUID id);

    List<DeliveryRouteLog> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);

    List<DeliveryRouteLog> findAllByDeliveryIdInAndDeletedAtIsNullOrderBySequenceNoAsc(List<UUID> deliveryIds);

    @Query("""
        select count(r)
        from DeliveryRouteLog r
        where r.deliveryManagerId = :deliveryManagerId
          and r.deletedAt is null
          and r.routeStatus not in (
              com.team.deliveryservice.delivery.domain.DeliveryRouteStatus.DELIVERED,
              com.team.deliveryservice.delivery.domain.DeliveryRouteStatus.CANCELLED
          )
    """)
    long countActiveByDeliveryManagerId(@Param("deliveryManagerId") UUID deliveryManagerId);
}
