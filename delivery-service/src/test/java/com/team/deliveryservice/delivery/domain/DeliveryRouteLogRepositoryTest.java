package com.team.deliveryservice.delivery.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.deliveryservice.support.PostgreSQLTestSupport;
import com.team.deliveryservice.support.TestJpaAuditingConfig;
import java.math.BigDecimal;
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
class DeliveryRouteLogRepositoryTest extends PostgreSQLTestSupport {

    @Autowired
    private DeliveryRouteLogRepository deliveryRouteLogRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    private static final UUID TEST_AUDITOR_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("활성 route log만 허브 배송 담당자 작업량으로 집계된다")
    void count_active_route_logs_only() {
        UUID managerId = UUID.randomUUID();
        UUID deliveryId = createAndSaveDelivery();

        deliveryRouteLogRepository.save(withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.WAITING_AT_HUB)));
        deliveryRouteLogRepository.save(withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.MOVING_BETWEEN_HUBS, 2)));
        deliveryRouteLogRepository.flush();

        long count = deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            managerId,
            List.of(DeliveryRouteStatus.DELIVERED, DeliveryRouteStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("DELIVERED 상태 route log는 허브 배송 담당체 활성 작업량에서 제외된다")
    void delivered_route_log_excluded() {
        UUID managerId = UUID.randomUUID();
        UUID deliveryId = createAndSaveDelivery();

        deliveryRouteLogRepository.save(withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.WAITING_AT_HUB)));
        deliveryRouteLogRepository.save(withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.DELIVERED, 2)));
        deliveryRouteLogRepository.flush();

        long count = deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            managerId,
            List.of(DeliveryRouteStatus.DELIVERED, DeliveryRouteStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CANCELLED 상태 route log는 허브 배송 담당자 활성 작업량에서 제외된다")
    void cancelled_route_log_excluded() {
        UUID managerId = UUID.randomUUID();
        UUID deliveryId = createAndSaveDelivery();

        deliveryRouteLogRepository.save(withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.WAITING_AT_HUB)));
        deliveryRouteLogRepository.save(withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.CANCELLED, 2)));
        deliveryRouteLogRepository.flush();

        long count = deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            managerId,
            List.of(DeliveryRouteStatus.DELIVERED, DeliveryRouteStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("soft delete 된 route log는 허브 배송 담당자 활성 작업량에서 제외된다")
    void soft_deleted_route_log_excluded() {
        UUID managerId = UUID.randomUUID();
        UUID deliveryId = createAndSaveDelivery();

        DeliveryRouteLog activeLog = withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.WAITING_AT_HUB));
        DeliveryRouteLog deletedLog = withAudit(createRouteLog(deliveryId, managerId, DeliveryRouteStatus.MOVING_BETWEEN_HUBS, 2));
        deletedLog.softDelete(UUID.fromString("00000000-0000-0000-0000-000000000009"));

        deliveryRouteLogRepository.save(activeLog);
        deliveryRouteLogRepository.save(deletedLog);
        deliveryRouteLogRepository.flush();

        long count = deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            managerId,
            List.of(DeliveryRouteStatus.DELIVERED, DeliveryRouteStatus.CANCELLED)
        );

        assertThat(count).isEqualTo(1);
    }

    private UUID createAndSaveDelivery() {
        Delivery delivery = Delivery.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .deliveryStatus(DeliveryStatus.WAITING_AT_HUB)
            .originHubId(UUID.randomUUID())
            .destinationHubId(UUID.randomUUID())
            .receiverCompanyId(UUID.randomUUID())
            .deliveryAddress("Test Address")
            .deliveryAddressDetail("101호")
            .recipientName("Test Recipient")
            .recipientSlackId("U12345678")
            .finalDispatchDeadlineAt(LocalDateTime.of(2026, 4, 1, 18, 0))
            .build();

        ReflectionTestUtils.setField(delivery, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(delivery, "createdBy", TEST_AUDITOR_ID);

        return deliveryRepository.saveAndFlush(delivery).getId();
    }

    private DeliveryRouteLog createRouteLog(UUID deliveryId, UUID managerId, DeliveryRouteStatus routeStatus) {
        return createRouteLog(deliveryId, managerId, routeStatus, 1);
    }

    private DeliveryRouteLog createRouteLog(UUID deliveryId, UUID managerId, DeliveryRouteStatus routeStatus, int sequenceNo) {
        return DeliveryRouteLog.builder()
            .id(UUID.randomUUID())
            .deliveryId(deliveryId)
            .sequenceNo(sequenceNo)
            .departureHubId(UUID.randomUUID())
            .arrivalHubId(UUID.randomUUID())
            .expectedDistanceKm(BigDecimal.valueOf(12.5))
            .expectedDurationMinutes(30)
            .realDurationMinutes(null)
            .routeStatus(routeStatus)
            .deliveryManagerId(managerId)
            .departedAt(null)
            .arrivedAt(null)
            .build();
    }

    private DeliveryRouteLog withAudit(DeliveryRouteLog routeLog) {
        ReflectionTestUtils.setField(routeLog, "createdAt", LocalDateTime.of(2026, 4, 1, 9, 0));
        ReflectionTestUtils.setField(routeLog, "createdBy", TEST_AUDITOR_ID);
        return routeLog;
    }
}
