package com.team.deliveryservice.delivery.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID>, DeliveryRepositoryCustom {

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    long countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
        UUID companyDeliveryManagerId,
        List<DeliveryStatus> deliveryStatuses
    );
}
