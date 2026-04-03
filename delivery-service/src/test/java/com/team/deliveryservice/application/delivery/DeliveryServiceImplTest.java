package com.team.deliveryservice.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.team.deliveryservice.delivery.application.dto.request.AssignCompanyDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.AssignHubDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.ChangeDeliveryStatusRequest;
import com.team.deliveryservice.delivery.application.dto.request.CreateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.delivery.application.service.DeliveryServiceImpl;
import com.team.deliveryservice.delivery.domain.Delivery;
import com.team.deliveryservice.delivery.domain.DeliveryRepository;
import com.team.deliveryservice.delivery.domain.DeliveryRouteLog;
import com.team.deliveryservice.delivery.domain.DeliveryRouteLogRepository;
import com.team.deliveryservice.delivery.domain.DeliveryRouteStatus;
import com.team.deliveryservice.delivery.domain.DeliveryStatus;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerRepository;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import com.team.deliveryservice.infrastructure.client.CompanyClient;
import com.team.deliveryservice.infrastructure.client.HubClient;
import com.team.deliveryservice.infrastructure.client.OrderClient;
import com.team.deliveryservice.infrastructure.client.dto.CompanyInternalResponse;
import com.team.deliveryservice.infrastructure.client.dto.HubExistsResponse;
import com.team.deliveryservice.infrastructure.client.dto.OrderInternalResponse;
import com.team.deliveryservice.infrastructure.client.dto.OptimalRouteResponseWrapper;
import feign.FeignException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private DeliveryRouteLogRepository deliveryRouteLogRepository;

    @Mock
    private HubClient hubClient;

    @Mock
    private CompanyClient companyClient;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    @Test
    @DisplayName("배송 생성 성공 - 허브/업체/주문 검증과 경로 로그 생성까지 수행")
    void create_delivery_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        // 주문 중복 여부 검증
        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);

        // 허브 존재 검증
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));

        // 업체 / 주문 검증
        given(companyClient.getCompany(receiverCompanyId)).willReturn(activeCompanyResponse(receiverCompanyId));
        given(orderClient.getOrder(orderId)).willReturn(readyForDeliveryOrderResponse(orderId, receiverCompanyId));

        // 배송 저장
        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // 최적 경로 조회
        given(hubClient.getOptimalRoute(eq(originHubId), eq(destinationHubId), any()))
            .willReturn(optimalRouteResponse(originHubId, destinationHubId));

        // route log 저장
        given(deliveryRouteLogRepository.saveAll(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        // 응답 반환용 route log 조회
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(any()))
            .willAnswer(invocation -> {
                UUID deliveryId = invocation.getArgument(0);
                return List.of(
                    DeliveryRouteLog.create(
                        deliveryId,
                        1,
                        originHubId,
                        destinationHubId,
                        BigDecimal.valueOf(12.5),
                        30,
                        null
                    )
                );
            });

        // when
        DeliveryResponse response = deliveryService.createDelivery(request);

        // then
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.deliveryAddress()).isEqualTo(request.deliveryAddress());
        assertThat(response.recipientName()).isEqualTo(request.recipientName());
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING_AT_HUB);
        assertThat(response.routeLogs()).hasSize(1);
        assertThat(response.routeLogs().get(0).sequenceNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("배송 생성 실패 - 이미 배송이 존재하는 주문")
    void create_delivery_fail_duplicate() {
        // given
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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 출발 허브가 존재하지 않음")
    void create_delivery_fail_origin_hub_not_found() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, false));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 수령 업체가 비활성 상태")
    void create_delivery_fail_company_inactive() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(new CompanyInternalResponse(
                receiverCompanyId,
                "비활성 업체",
                "RECEIVER",
                destinationHubId,
                false
            ));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 주문 상태가 READY_FOR_DELIVERY 가 아님")
    void create_delivery_fail_order_status_not_allowed() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId)).willReturn(activeCompanyResponse(receiverCompanyId));
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                receiverCompanyId,
                null,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "CONFIRMED"
            ));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_CREATE_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 주문의 receiverCompanyId 와 요청 receiverCompanyId 가 다름")
    void create_delivery_fail_receiver_company_mismatch() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID anotherReceiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId)).willReturn(activeCompanyResponse(receiverCompanyId));
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                anotherReceiverCompanyId,
                null,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "READY_FOR_DELIVERY"
            ));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 주문에 이미 deliveryId 가 연결되어 있음")
    void create_delivery_fail_order_already_has_delivery_id() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId)).willReturn(activeCompanyResponse(receiverCompanyId));
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                receiverCompanyId,
                deliveryId,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "READY_FOR_DELIVERY"
            ));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 최적 경로가 비어 있음")
    void create_delivery_fail_empty_route() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId)).willReturn(activeCompanyResponse(receiverCompanyId));
        given(orderClient.getOrder(orderId)).willReturn(readyForDeliveryOrderResponse(orderId, receiverCompanyId));
        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        given(hubClient.getOptimalRoute(eq(originHubId), eq(destinationHubId), any()))
            .willReturn(new OptimalRouteResponseWrapper(
                200,
                "성공",
                new OptimalRouteResponseWrapper.OptimalRouteResponse(
                    originHubId,
                    destinationHubId,
                    0,
                    0.0,
                    List.of()
                )
            ));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - hubClient exists 호출 중 예외가 발생하면 HUB_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_hub_client_exists_throws_exception() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any()))
            .willThrow(feignBadRequestException());

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - companyClient 호출 중 예외가 발생하면 COMPANY_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_company_client_throws_exception() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId))
            .willThrow(feignBadRequestException());

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - orderClient 호출 중 예외가 발생하면 ORDER_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_order_client_throws_exception() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId)).willReturn(activeCompanyResponse(receiverCompanyId));
        given(orderClient.getOrder(orderId))
            .willThrow(feignBadRequestException());

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - hubClient optimal route 호출 중 예외가 발생하면 HUB_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_hub_client_route_throws_exception() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(companyClient.getCompany(receiverCompanyId)).willReturn(activeCompanyResponse(receiverCompanyId));
        given(orderClient.getOrder(orderId)).willReturn(readyForDeliveryOrderResponse(orderId, receiverCompanyId));
        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(hubClient.getOptimalRoute(eq(originHubId), eq(destinationHubId), any()))
            .willThrow(feignBadRequestException());

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 상태 변경 성공 - WAITING_AT_HUB 에서 MOVING_BETWEEN_HUBS 로 변경")
    void change_delivery_status_success() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();
        ChangeDeliveryStatusRequest request = new ChangeDeliveryStatusRequest(
            DeliveryStatus.MOVING_BETWEEN_HUBS
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        // when
        DeliveryResponse response = deliveryService.changeDeliveryStatus(deliveryId, request);

        // then
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.MOVING_BETWEEN_HUBS);
        assertThat(delivery.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("배송 상태 변경 실패 - 허용되지 않은 상태 전이")
    void change_delivery_status_fail_invalid_transition() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();
        ChangeDeliveryStatusRequest request = new ChangeDeliveryStatusRequest(
            DeliveryStatus.DELIVERED
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when & then
        assertThatThrownBy(() -> deliveryService.changeDeliveryStatus(deliveryId, request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_STATUS_CHANGE_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("배송 취소 성공 - WAITING_AT_HUB 상태에서만 가능")
    void cancel_delivery_success() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        // when
        DeliveryResponse response = deliveryService.cancelDelivery(deliveryId);

        // then
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("배송 취소 실패 - WAITING_AT_HUB 이외 상태에서는 불가")
    void cancel_delivery_fail_when_not_waiting() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();
        delivery.updateStatus(DeliveryStatus.MOVING_BETWEEN_HUBS);

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        // when & then
        assertThatThrownBy(() -> deliveryService.cancelDelivery(deliveryId))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_CANCEL_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 성공")
    void assign_company_delivery_manager_success() {
        // given
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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createCompanyDeliveryManager(destinationHubId);
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        // when
        DeliveryResponse response = deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser);

        // then
        assertThat(response.companyDeliveryManagerId()).isEqualTo(manager.getId());
        assertThat(delivery.getCompanyDeliveryManagerId()).isEqualTo(manager.getId());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 배송 담당자를 찾을 수 없음")
    void assign_company_delivery_manager_fail_not_found() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", UUID.randomUUID(), null);

        Delivery delivery = createDelivery();
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(managerId);

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(managerId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 타입이 업체 배송 담당자가 아님")
    void assign_company_delivery_manager_fail_invalid_type() {
        // given
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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createHubDeliveryManager();
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        // when & then
        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 배송 목적지 허브와 담당자 소속 허브가 다름")
    void assign_company_delivery_manager_fail_hub_mismatch() {
        // given
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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createCompanyDeliveryManager(anotherHubId);
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        // when & then
        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_HUB_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("업체 배송 담당자 배정 실패 - 취소된 배송에는 배정할 수 없음")
    void assign_company_delivery_manager_fail_when_cancelled() {
        // given
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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );
        delivery.cancel();

        DeliveryManager manager = createCompanyDeliveryManager(destinationHubId);
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryManagerRepository.findByIdAndDeletedAtIsNull(manager.getId()))
            .willReturn(Optional.of(manager));

        // when & then
        assertThatThrownBy(() -> deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ASSIGN_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 성공")
    void assign_hub_delivery_manager_success() {
        // given
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

        // when
        DeliveryResponse response = deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser);

        // then
        assertThat(routeLog.getDeliveryManagerId()).isEqualTo(manager.getId());
        assertThat(response.routeLogs()).hasSize(1);
        assertThat(response.routeLogs().get(0).deliveryManagerId()).isEqualTo(manager.getId());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 실패 - routeLog 가 해당 배송 소속이 아님")
    void assign_hub_delivery_manager_fail_route_log_delivery_mismatch() {
        // given
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

        // when & then
        assertThatThrownBy(() -> deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 실패 - 타입이 허브 배송 담당자가 아님")
    void assign_hub_delivery_manager_fail_invalid_type() {
        // given
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

        // when & then
        assertThatThrownBy(() -> deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID.getMessage());
    }

    @Test
    @DisplayName("허브 배송 담당자 배정 실패 - 완료된 배송 경로에는 배정할 수 없음")
    void assign_hub_delivery_manager_fail_when_route_delivered() {
        // given
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

        // when & then
        assertThatThrownBy(() -> deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("배송 삭제 시 배송 경로 로그도 함께 soft delete")
    void delete_delivery_soft_delete_route_logs() {
        // given
        UUID deliveryId = UUID.randomUUID();

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

        // when
        deliveryService.deleteDelivery(deliveryId);

        // then
        assertThat(delivery.getDeletedAt()).isNotNull();
        assertThat(delivery.getDeletedBy()).isEqualTo(SYSTEM_ACTOR_ID);
        assertThat(routeLog.getDeletedAt()).isNotNull();
        assertThat(routeLog.getDeletedBy()).isEqualTo(SYSTEM_ACTOR_ID);
    }

    // 테스트용 배송 엔티티 생성
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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );
    }

    // 테스트용 업체 배송 담당자 생성
    private DeliveryManager createCompanyDeliveryManager(UUID hubId) {
        return DeliveryManager.create(
            UUID.randomUUID(),
            hubId,
            "U123COMPANY",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
            1
        );
    }

    // 테스트용 허브 배송 담당자 생성
    private DeliveryManager createHubDeliveryManager() {
        return DeliveryManager.create(
            UUID.randomUUID(),
            null,
            "U123HUB",
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            1
        );
    }

    // 허브 존재 응답 생성
    private HubExistsResponse hubExistsResponse(UUID hubId, boolean exists) {
        return new HubExistsResponse(
            true,
            new HubExistsResponse.HubExistsData(hubId, exists),
            "SUCCESS",
            "요청이 성공했습니다."
        );
    }

    // 활성 업체 응답 생성
    private CompanyInternalResponse activeCompanyResponse(UUID companyId) {
        return new CompanyInternalResponse(
            companyId,
            "수령 업체",
            "RECEIVER",
            UUID.randomUUID(),
            true
        );
    }

    // 배송 생성 가능한 주문 응답 생성
    private OrderInternalResponse readyForDeliveryOrderResponse(UUID orderId, UUID receiverCompanyId) {
        return new OrderInternalResponse(
            orderId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            receiverCompanyId,
            null,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "READY_FOR_DELIVERY"
        );
    }

    // 최적 경로 응답 생성
    private OptimalRouteResponseWrapper optimalRouteResponse(UUID originHubId, UUID destinationHubId) {
        return new OptimalRouteResponseWrapper(
            200,
            "허브 최적 경로 조회를 성공했습니다.",
            new OptimalRouteResponseWrapper.OptimalRouteResponse(
                originHubId,
                destinationHubId,
                30,
                12.5,
                List.of(
                    new OptimalRouteResponseWrapper.RoutePathResponse(
                        1,
                        originHubId,
                        destinationHubId,
                        30,
                        12.5
                    )
                )
            )
        );
    }

    // 공통 Feign 예외 생성
    private FeignException feignBadRequestException() {
        return FeignException.errorStatus(
            "test",
            feign.Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(feign.Request.create(
                    feign.Request.HttpMethod.GET,
                    "http://localhost/test",
                    java.util.Map.of(),
                    null,
                    StandardCharsets.UTF_8,
                    null
                ))
                .headers(java.util.Map.of())
                .build()
        );
    }
}
