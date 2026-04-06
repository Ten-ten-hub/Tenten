package com.team.deliveryservice.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.team.deliveryservice.delivery.application.service.DeliveryManagerAutoAssignServiceImpl;
import com.team.deliveryservice.delivery.domain.Delivery;
import com.team.deliveryservice.delivery.domain.DeliveryRepository;
import com.team.deliveryservice.delivery.domain.DeliveryRouteLog;
import com.team.deliveryservice.delivery.domain.DeliveryRouteLogRepository;
import com.team.deliveryservice.delivery.domain.DeliveryRouteStatus;
import com.team.deliveryservice.delivery.domain.DeliveryStatus;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerRepository;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerAutoAssignServiceImplTest {

    private static final List<DeliveryRouteStatus> INACTIVE_ROUTE_STATUSES =
        List.of(DeliveryRouteStatus.DELIVERED, DeliveryRouteStatus.CANCELLED);

    private static final List<DeliveryStatus> INACTIVE_DELIVERY_STATUSES =
        List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED);

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private DeliveryRouteLogRepository deliveryRouteLogRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryManagerAutoAssignServiceImpl deliveryManagerAutoAssignService;

    @Test
    @DisplayName("자동 배정 성공 - 허브 배송 담당자는 활성 작업량이 더 적은 담당자가 선택된다")
    void auto_assign_hub_manager_success_by_lower_active_count() {
        UUID deliveryId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery = createDelivery(deliveryId, departureHubId, destinationHubId);
        DeliveryRouteLog routeLog = createRouteLog(deliveryId, 1, departureHubId, UUID.randomUUID());

        DeliveryManager highLoadManager = createHubManager(UUID.randomUUID(), departureHubId, 0);
        DeliveryManager lowLoadManager = createHubManager(UUID.randomUUID(), departureHubId, 1);
        DeliveryManager companyManager = createCompanyManager(UUID.randomUUID(), destinationHubId, 0);

        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            departureHubId
        )).willReturn(List.of(highLoadManager, lowLoadManager));

        given(deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            highLoadManager.getId(),
            INACTIVE_ROUTE_STATUSES
        )).willReturn(5L);

        given(deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            lowLoadManager.getId(),
            INACTIVE_ROUTE_STATUSES
        )).willReturn(1L);

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            destinationHubId
        )).willReturn(List.of(companyManager));

        // when
        deliveryManagerAutoAssignService.autoAssign(delivery);

        // then
        assertThat(routeLog.getDeliveryManagerId()).isEqualTo(lowLoadManager.getId());
        assertThat(delivery.getCompanyDeliveryManagerId()).isEqualTo(companyManager.getId());
    }

    @Test
    @DisplayName("자동 배정 성공 - 허브 배송 담당자는 활성 작업량이 같으면 sequence가 더 작은 담당자가 선택된다")
    void auto_assign_hub_manager_success_by_lower_sequence_when_same_load() {
        UUID deliveryId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery = createDelivery(deliveryId, departureHubId, destinationHubId);
        DeliveryRouteLog routeLog = createRouteLog(deliveryId, 1, departureHubId, UUID.randomUUID());

        DeliveryManager sequenceTwoManager = createHubManager(UUID.randomUUID(), departureHubId, 2);
        DeliveryManager sequenceZeroManager = createHubManager(UUID.randomUUID(), departureHubId, 0);
        DeliveryManager companyManager = createCompanyManager(UUID.randomUUID(), destinationHubId, 0);

        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            departureHubId
        )).willReturn(List.of(sequenceZeroManager, sequenceTwoManager));

        given(deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            sequenceZeroManager.getId(),
            INACTIVE_ROUTE_STATUSES
        )).willReturn(3L);

        given(deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            sequenceTwoManager.getId(),
            INACTIVE_ROUTE_STATUSES
        )).willReturn(3L);

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            destinationHubId
        )).willReturn(List.of(companyManager));

        // when
        deliveryManagerAutoAssignService.autoAssign(delivery);

        // then
        assertThat(routeLog.getDeliveryManagerId()).isEqualTo(sequenceZeroManager.getId());
        assertThat(delivery.getCompanyDeliveryManagerId()).isEqualTo(companyManager.getId());
    }

    @Test
    @DisplayName("자동 배정 성공 - 업체 배송 담당자는 활성 작업량이 더 적은 담당자가 선택된다")
    void auto_assign_company_manager_success_by_lower_active_count() {
        UUID deliveryId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery = createDelivery(deliveryId, originHubId, destinationHubId);
        DeliveryRouteLog routeLog = createRouteLog(deliveryId, 1, originHubId, destinationHubId);

        DeliveryManager hubManager = createHubManager(UUID.randomUUID(), originHubId, 0);
        DeliveryManager highLoadCompanyManager = createCompanyManager(UUID.randomUUID(), destinationHubId, 0);
        DeliveryManager lowLoadCompanyManager = createCompanyManager(UUID.randomUUID(), destinationHubId, 1);

        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            originHubId
        )).willReturn(List.of(hubManager));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            destinationHubId
        )).willReturn(List.of(highLoadCompanyManager, lowLoadCompanyManager));

        given(deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            highLoadCompanyManager.getId(),
            INACTIVE_DELIVERY_STATUSES
        )).willReturn(4L);

        given(deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            lowLoadCompanyManager.getId(),
            INACTIVE_DELIVERY_STATUSES
        )).willReturn(1L);

        // when
        deliveryManagerAutoAssignService.autoAssign(delivery);

        // then
        assertThat(routeLog.getDeliveryManagerId()).isEqualTo(hubManager.getId());
        assertThat(delivery.getCompanyDeliveryManagerId()).isEqualTo(lowLoadCompanyManager.getId());
    }

    @Test
    @DisplayName("자동 배정 성공 - 업체 배송 담당자는 활성 작업량이 같으면 sequence가 더 작은 담당자가 선택된다")
    void auto_assign_company_manager_success_by_lower_sequence_when_same_load() {
        UUID deliveryId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery = createDelivery(deliveryId, originHubId, destinationHubId);
        DeliveryRouteLog routeLog = createRouteLog(deliveryId, 1, originHubId, destinationHubId);

        DeliveryManager hubManager = createHubManager(UUID.randomUUID(), originHubId, 0);
        DeliveryManager sequenceThreeManager = createCompanyManager(UUID.randomUUID(), destinationHubId, 3);
        DeliveryManager sequenceOneManager = createCompanyManager(UUID.randomUUID(), destinationHubId, 1);

        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            originHubId
        )).willReturn(List.of(hubManager));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            destinationHubId
        )).willReturn(List.of(sequenceOneManager, sequenceThreeManager));

        given(deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            sequenceOneManager.getId(),
            INACTIVE_DELIVERY_STATUSES
        )).willReturn(2L);

        given(deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            sequenceThreeManager.getId(),
            INACTIVE_DELIVERY_STATUSES
        )).willReturn(2L);

        // when
        deliveryManagerAutoAssignService.autoAssign(delivery);

        // then
        assertThat(delivery.getCompanyDeliveryManagerId()).isEqualTo(sequenceOneManager.getId());
        assertThat(routeLog.getDeliveryManagerId()).isEqualTo(hubManager.getId());
    }

    @Test
    @DisplayName("자동 배정 실패 - 허브 배송 담당자 후보가 없으면 예외 발생")
    void auto_assign_fail_when_no_hub_manager_candidate() {
        UUID deliveryId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery = createDelivery(deliveryId, departureHubId, destinationHubId);
        DeliveryRouteLog routeLog = createRouteLog(deliveryId, 1, departureHubId, UUID.randomUUID());

        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            departureHubId
        )).willReturn(List.of());

        assertThatThrownBy(() -> deliveryManagerAutoAssignService.autoAssign(delivery))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("자동 배정 실패 - 업체 배송 담당자 후보가 없으면 예외 발생")
    void auto_assign_fail_when_no_company_manager_candidate() {
        UUID deliveryId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery = createDelivery(deliveryId, originHubId, destinationHubId);
        DeliveryRouteLog routeLog = createRouteLog(deliveryId, 1, originHubId, destinationHubId);

        DeliveryManager hubManager = createHubManager(UUID.randomUUID(), originHubId, 0);

        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            originHubId
        )).willReturn(List.of(hubManager));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            destinationHubId
        )).willReturn(List.of());

        assertThatThrownBy(() -> deliveryManagerAutoAssignService.autoAssign(delivery))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("자동 배정 성공 - route log가 여러 개이면 각 출발 허브 기준으로 허브 배송 담당자가 각각 배정된다")
    void auto_assign_success_for_multiple_route_logs() {
        UUID deliveryId = UUID.randomUUID();
        UUID firstHubId = UUID.randomUUID();
        UUID secondHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        Delivery delivery = createDelivery(deliveryId, firstHubId, destinationHubId);

        DeliveryRouteLog firstRouteLog = createRouteLog(deliveryId, 1, firstHubId, secondHubId);
        DeliveryRouteLog secondRouteLog = createRouteLog(deliveryId, 2, secondHubId, destinationHubId);

        DeliveryManager firstHubManager = createHubManager(UUID.randomUUID(), firstHubId, 0);
        DeliveryManager secondHubManager = createHubManager(UUID.randomUUID(), secondHubId, 0);
        DeliveryManager companyManager = createCompanyManager(UUID.randomUUID(), destinationHubId, 0);

        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(firstRouteLog, secondRouteLog));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            firstHubId
        )).willReturn(List.of(firstHubManager));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            secondHubId
        )).willReturn(List.of(secondHubManager));

        given(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            destinationHubId
        )).willReturn(List.of(companyManager));

        // when
        deliveryManagerAutoAssignService.autoAssign(delivery);

        // then
        assertThat(firstRouteLog.getDeliveryManagerId()).isEqualTo(firstHubManager.getId());
        assertThat(secondRouteLog.getDeliveryManagerId()).isEqualTo(secondHubManager.getId());
        assertThat(delivery.getCompanyDeliveryManagerId()).isEqualTo(companyManager.getId());
    }

    private Delivery createDelivery(UUID deliveryId, UUID originHubId, UUID destinationHubId) {
        return Delivery.builder()
            .id(deliveryId)
            .orderId(UUID.randomUUID())
            .deliveryStatus(DeliveryStatus.WAITING_AT_HUB)
            .originHubId(originHubId)
            .destinationHubId(destinationHubId)
            .receiverCompanyId(UUID.randomUUID())
            .deliveryAddress("서울시 강남구 테헤란로 123")
            .deliveryAddressDetail("101호")
            .recipientName("홍길동")
            .recipientSlackId("U12345678")
            .finalDispatchDeadlineAt(LocalDateTime.of(2026, 4, 1, 18, 0))
            .build();
    }

    private DeliveryRouteLog createRouteLog(
        UUID deliveryId,
        int sequenceNo,
        UUID departureHubId,
        UUID arrivalHubId
    ) {
        return DeliveryRouteLog.create(
            deliveryId,
            sequenceNo,
            departureHubId,
            arrivalHubId,
            BigDecimal.valueOf(12.5),
            30,
            null
        );
    }

    private DeliveryManager createHubManager(UUID id, UUID hubId, int sequence) {
        return DeliveryManager.create(
            id,
            hubId,
            "U123HUB",
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            sequence
        );
    }

    private DeliveryManager createCompanyManager(UUID id, UUID hubId, int sequence) {
        return DeliveryManager.create(
            id,
            hubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            sequence
        );
    }
}
