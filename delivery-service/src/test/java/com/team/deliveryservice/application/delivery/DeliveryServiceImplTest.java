package com.team.deliveryservice.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team.deliveryservice.domain.delivery.Delivery;
import com.team.deliveryservice.domain.delivery.DeliveryRepository;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLog;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLogRepository;
import com.team.deliveryservice.domain.delivery.DeliveryRouteStatus;
import com.team.deliveryservice.domain.delivery.DeliveryStatus;
import com.team.deliveryservice.domain.deliverymanager.DeliveryManager;
import com.team.deliveryservice.domain.deliverymanager.DeliveryManagerRepository;
import com.team.deliveryservice.domain.deliverymanager.DeliveryManagerType;
import com.team.deliveryservice.presentation.common.CurrentUser;
import com.team.deliveryservice.presentation.common.DeliveryErrorCode;
import com.team.deliveryservice.presentation.common.ServiceException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private DeliveryRouteLogRepository deliveryRouteLogRepository;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    @Test
    @DisplayName("배송 생성 성공")
    void create_delivery_success() {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            UUID.randomUUID(),
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())).willReturn(false);
        given(deliveryRepository.save(any(Delivery.class))).willAnswer(invocation -> invocation.getArgument(0));

        DeliveryResponse response = deliveryService.createDelivery(request, currentUser);

        assertThat(response.orderId()).isEqualTo(request.orderId());
        assertThat(response.deliveryAddress()).isEqualTo(request.deliveryAddress());
        assertThat(response.recipientName()).isEqualTo(request.recipientName());
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING_AT_HUB);
    }

    @Test
    @DisplayName("배송 생성 실패 - 이미 배송이 존재하는 주문")
    void create_delivery_fail_duplicate() {
        UUID orderId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            UUID.randomUUID(),
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(true);

        assertThatThrownBy(() -> deliveryService.createDelivery(request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송 상태 변경 성공 - WAITING_AT_HUB 에서 MOVING_BETWEEN_HUBS 로 변경")
    void change_delivery_status_success() {
        UUID deliveryId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        ChangeDeliveryStatusRequest request = new ChangeDeliveryStatusRequest(
            DeliveryStatus.MOVING_BETWEEN_HUBS
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        DeliveryResponse response = deliveryService.changeDeliveryStatus(deliveryId, request, currentUser);

        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.MOVING_BETWEEN_HUBS);
        assertThat(delivery.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("배송 상태 변경 실패 - 허용되지 않은 상태 전이")
    void change_delivery_status_fail_invalid_transition() {
        UUID deliveryId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        ChangeDeliveryStatusRequest request = new ChangeDeliveryStatusRequest(
            DeliveryStatus.DELIVERED
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.changeDeliveryStatus(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_STATUS_CHANGE_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("배송 취소 성공 - WAITING_AT_HUB 상태에서만 가능")
    void cancel_delivery_success() {
        UUID deliveryId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "COMPANY_MANAGER", null, null);

        Delivery delivery = createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        DeliveryResponse response = deliveryService.cancelDelivery(deliveryId, currentUser);

        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("배송 취소 실패 - WAITING_AT_HUB 이외 상태에서는 불가")
    void cancel_delivery_fail_when_not_waiting() {
        UUID deliveryId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "COMPANY_MANAGER", null, null);

        Delivery delivery = createDelivery();
        delivery.updateStatus(DeliveryStatus.MOVING_BETWEEN_HUBS);

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(deliveryId, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_CANCEL_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 성공")
    void assign_company_delivery_manager_success() {
        UUID deliveryId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", destinationHubId, null);

        Delivery delivery = Delivery.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            destinationHubId,
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            null,
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createCompanyDeliveryManager(destinationHubId);
        AssignCompanyDeliveryManagerRequest request =
            new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        DeliveryResponse response = deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser);

        assertThat(response.companyDeliveryManagerId()).isEqualTo(manager.getId());
        assertThat(delivery.getCompanyDeliveryManagerId()).isEqualTo(manager.getId());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 배송 담당자를 찾을 수 없음")
    void assign_company_delivery_manager_fail_not_found() {
        UUID deliveryId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", UUID.randomUUID(), null);

        Delivery delivery = createDelivery();
        AssignCompanyDeliveryManagerRequest request =
            new AssignCompanyDeliveryManagerRequest(managerId);

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(managerId))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 타입이 업체 배송 담당자가 아님")
    void assign_company_delivery_manager_fail_invalid_type() {
        UUID deliveryId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", destinationHubId, null);

        Delivery delivery = Delivery.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            destinationHubId,
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            null,
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createHubDeliveryManager();
        AssignCompanyDeliveryManagerRequest request =
            new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 배송 목적지 허브와 담당자 소속 허브가 다름")
    void assign_company_delivery_manager_fail_hub_mismatch() {
        UUID deliveryId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID anotherHubId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", destinationHubId, null);

        Delivery delivery = Delivery.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            destinationHubId,
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            null,
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createCompanyDeliveryManager(anotherHubId);
        AssignCompanyDeliveryManagerRequest request =
            new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_HUB_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 취소된 배송에는 배정할 수 없음")
    void assign_company_delivery_manager_fail_when_cancelled() {
        UUID deliveryId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", destinationHubId, null);

        Delivery delivery = Delivery.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            destinationHubId,
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            null,
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );
        delivery.cancel();

        DeliveryManager manager = createCompanyDeliveryManager(destinationHubId);
        AssignCompanyDeliveryManagerRequest request =
            new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ASSIGN_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 성공")
    void assign_hub_delivery_manager_success() {
        UUID deliveryId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        DeliveryRouteLog routeLog = DeliveryRouteLog.create(
            delivery.getId(),
            1,
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(12.5),
            30,
            null
        );

        DeliveryManager manager = createHubDeliveryManager();
        AssignHubDeliveryManagerRequest request =
            new AssignHubDeliveryManagerRequest(routeLog.getId(), manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findByIdAndDeletedAtIsNull(routeLog.getId()))
            .willReturn(Optional.of(routeLog));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        DeliveryResponse response = deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser);

        assertThat(routeLog.getDeliveryManagerId()).isEqualTo(manager.getId());
        assertThat(response.routeLogs()).hasSize(1);
        assertThat(response.routeLogs().get(0).deliveryManagerId()).isEqualTo(manager.getId());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 실패 - routeLog 가 해당 배송 소속이 아님")
    void assign_hub_delivery_manager_fail_route_log_delivery_mismatch() {
        UUID deliveryId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        DeliveryRouteLog anotherDeliveryRouteLog = DeliveryRouteLog.create(
            UUID.randomUUID(),
            1,
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(12.5),
            30,
            null
        );

        DeliveryManager manager = createHubDeliveryManager();
        AssignHubDeliveryManagerRequest request =
            new AssignHubDeliveryManagerRequest(anotherDeliveryRouteLog.getId(), manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findByIdAndDeletedAtIsNull(anotherDeliveryRouteLog.getId()))
            .willReturn(Optional.of(anotherDeliveryRouteLog));

        assertThatThrownBy(() -> deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 실패 - 타입이 허브 배송 담당자가 아님")
    void assign_hub_delivery_manager_fail_invalid_type() {
        UUID deliveryId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        DeliveryRouteLog routeLog = DeliveryRouteLog.create(
            delivery.getId(),
            1,
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(12.5),
            30,
            null
        );

        DeliveryManager manager = createCompanyDeliveryManager(destinationHubId);
        AssignHubDeliveryManagerRequest request =
            new AssignHubDeliveryManagerRequest(routeLog.getId(), manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findByIdAndDeletedAtIsNull(routeLog.getId()))
            .willReturn(Optional.of(routeLog));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID.getMessage());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 실패 - 완료된 배송 경로에는 배정할 수 없음")
    void assign_hub_delivery_manager_fail_when_route_delivered() {
        UUID deliveryId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        DeliveryRouteLog routeLog = DeliveryRouteLog.builder()
            .id(UUID.randomUUID())
            .deliveryId(delivery.getId())
            .sequenceNo(1)
            .departureHubId(UUID.randomUUID())
            .arrivalHubId(UUID.randomUUID())
            .expectedDistanceKm(BigDecimal.valueOf(12.5))
            .expectedDurationMinutes(30)
            .routeStatus(DeliveryRouteStatus.DELIVERED)
            .deliveryManagerId(null)
            .departedAt(LocalDateTime.now().minusHours(1))
            .arrivedAt(LocalDateTime.now())
            .build();

        DeliveryManager manager = createHubDeliveryManager();
        AssignHubDeliveryManagerRequest request =
            new AssignHubDeliveryManagerRequest(routeLog.getId(), manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findByIdAndDeletedAtIsNull(routeLog.getId()))
            .willReturn(Optional.of(routeLog));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        assertThatThrownBy(() -> deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("배송 삭제 시 배송 경로 로그도 함께 soft delete")
    void delete_delivery_soft_delete_route_logs() {
        UUID deliveryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CurrentUser currentUser = new CurrentUser(userId, "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();

        DeliveryRouteLog routeLog = DeliveryRouteLog.create(
            delivery.getId(),
            1,
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(12.5),
            30,
            UUID.randomUUID()
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of(routeLog));

        deliveryService.deleteDelivery(deliveryId, currentUser);

        assertThat(delivery.getDeletedAt()).isNotNull();
        assertThat(delivery.getDeletedBy()).isEqualTo(userId);
        assertThat(routeLog.getDeletedAt()).isNotNull();
        assertThat(routeLog.getDeletedBy()).isEqualTo(userId);
    }

    private Delivery createDelivery() {
        return Delivery.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            UUID.randomUUID(),
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );
    }

    private DeliveryManager createCompanyDeliveryManager(UUID hubId) {
        return DeliveryManager.create(
            UUID.randomUUID(),
            hubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            1
        );
    }

    private DeliveryManager createHubDeliveryManager() {
        return DeliveryManager.create(
            UUID.randomUUID(),
            null,
            "U123HUB",
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            1
        );
    }
}
