package com.team.deliveryservice.delivery.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.deliveryservice.support.PostgreSQLTestSupport;
import com.team.deliveryservice.support.TestJpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
class DeliveryRepositoryTest extends PostgreSQLTestSupport {

    @Autowired
    private DeliveryRepository deliveryRepository;

    private static final UUID TEST_AUDITOR_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("활성 delivery만 업체 배송 담당자 작업량으로 집계된다")
    void count_active_company_assignments_only() {
        UUID managerId = UUID.randomUUID();

        deliveryRepository.save(withAudit(createDelivery(managerId, DeliveryStatus.WAITING_AT_HUB)));
        deliveryRepository.save(withAudit(createDelivery(managerId, DeliveryStatus.OUT_FOR_DELIVERY)));
        deliveryRepository.flush();

        long count = deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            managerId,
            List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("DELIVERED 상태 delivery는 업체 배송 담당자 활성 작업량에서 제외된다")
    void delivered_delivery_excluded() {
        UUID managerId = UUID.randomUUID();

        deliveryRepository.save(withAudit(createDelivery(managerId, DeliveryStatus.WAITING_AT_HUB)));
        deliveryRepository.save(withAudit(createDelivery(managerId, DeliveryStatus.DELIVERED)));
        deliveryRepository.flush();

        long count = deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            managerId,
            List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CANCELLED 상태 delivery는 업체 배송 담당자 활성 작업량에서 제외된다")
    void cancelled_delivery_excluded() {
        UUID managerId = UUID.randomUUID();

        deliveryRepository.save(withAudit(createDelivery(managerId, DeliveryStatus.WAITING_AT_HUB)));
        deliveryRepository.save(withAudit(createDelivery(managerId, DeliveryStatus.CANCELLED)));
        deliveryRepository.flush();

        long count = deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            managerId,
            List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("soft delete 된 delivery는 업체 배송 담당자 활성 작업량에서 제외된다")
    void soft_deleted_delivery_excluded() {
        UUID managerId = UUID.randomUUID();

        Delivery activeDelivery = withAudit(createDelivery(managerId, DeliveryStatus.WAITING_AT_HUB));
        Delivery deletedDelivery = withAudit(createDelivery(managerId, DeliveryStatus.MOVING_BETWEEN_HUBS));
        deletedDelivery.softDelete(UUID.fromString("00000000-0000-0000-0000-000000000009"));

        deliveryRepository.save(activeDelivery);
        deliveryRepository.save(deletedDelivery);
        deliveryRepository.flush();

        long count = deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            managerId,
            List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(1);
    }

    private Delivery createDelivery(UUID companyManagerId, DeliveryStatus status) {
        return Delivery.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .deliveryStatus(status)
            .originHubId(UUID.randomUUID())
            .destinationHubId(UUID.randomUUID())
            .receiverCompanyId(UUID.randomUUID())
            .deliveryAddress("서울시 강남구 테헤란로 123")
            .deliveryAddressDetail("101호")
            .recipientName("홍길동")
            .recipientSlackId("U12345678")
            .companyDeliveryManagerId(companyManagerId)
            .finalDispatchDeadlineAt(LocalDateTime.of(2026, 4, 1, 18, 0))
            .build();
    }

    private Delivery withAudit(Delivery delivery) {
        ReflectionTestUtils.setField(delivery, "createdAt", LocalDateTime.of(2026, 4, 1, 9, 0));
        ReflectionTestUtils.setField(delivery, "createdBy", TEST_AUDITOR_ID);
        return delivery;
    }
}
