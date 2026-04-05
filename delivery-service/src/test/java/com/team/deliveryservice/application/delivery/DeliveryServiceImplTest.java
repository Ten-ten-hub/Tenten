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
import com.team.deliveryservice.delivery.application.dto.response.AiDeliveryResponse;
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
import com.team.deliveryservice.infrastructure.client.dto.HubInternalResponse;
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

    private static final UUID SYSTEM_ACTOR_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000000");

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
    @DisplayName("배송 생성 성공 - 주문/업체/허브 검증 후 배송 및 경로 로그 생성")
    void create_delivery_success() {
        UUID orderId = UUID.randomUUID();
        UUID orderedBy = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            orderedBy,
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "문 앞에 놓아주세요"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);

        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));

        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, originHubId));

        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(activeReceiverCompanyResponse(receiverCompanyId, destinationHubId));

        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));

        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        given(hubClient.getOptimalRoute(eq(originHubId), eq(destinationHubId), any()))
            .willReturn(optimalRouteResponse(originHubId, destinationHubId));

        given(deliveryRouteLogRepository.saveAll(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

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

        DeliveryResponse response = deliveryService.createDelivery(request);

        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.deliveryAddress()).isEqualTo("서울시 강남구 테헤란로 123");
        assertThat(response.deliveryAddressDetail()).isEqualTo("101호");
        assertThat(response.recipientName()).isEqualTo("홍길동");
        assertThat(response.recipientSlackId()).isEqualTo("U12345678");
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING_AT_HUB);
        assertThat(response.routeLogs()).hasSize(1);
        assertThat(response.routeLogs().get(0).sequenceNo()).isEqualTo(1);
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
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(true);

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 공급 업체가 비활성 상태")
    void create_delivery_fail_supplier_company_inactive() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));

        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(new CompanyInternalResponse(
                supplierCompanyId,
                "비활성 공급업체",
                "PRODUCER",
                UUID.randomUUID(),
                "서울시 송파구 올림픽로 1",
                "201호",
                "공급담당자",
                "U_SUPPLIER",
                false
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 수령 업체가 비활성 상태")
    void create_delivery_fail_receiver_company_inactive() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));

        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, originHubId));

        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(new CompanyInternalResponse(
                receiverCompanyId,
                "비활성 수령업체",
                "RECEIVER",
                UUID.randomUUID(),
                "서울시 강남구 테헤란로 123",
                "101호",
                "홍길동",
                "U12345678",
                false
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 주문 상태가 READY_FOR_DELIVERY 가 아님")
    void create_delivery_fail_order_status_not_allowed() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);

        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                supplierCompanyId,
                receiverCompanyId,
                null,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "배송 전에 확인 연락 부탁드립니다.",
                "신선 전복 세트",
                "CONFIRMED"
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_CREATE_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 주문의 supplierCompanyId 와 요청 supplierCompanyId 가 다름")
    void create_delivery_fail_supplier_company_mismatch() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID anotherSupplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                anotherSupplierCompanyId,
                receiverCompanyId,
                null,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "배송 전에 확인 연락 부탁드립니다.",
                "신선 전복 세트",
                "READY_FOR_DELIVERY"
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 주문의 receiverCompanyId 와 요청 receiverCompanyId 가 다름")
    void create_delivery_fail_receiver_company_mismatch() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID anotherReceiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                supplierCompanyId,
                anotherReceiverCompanyId,
                null,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "배송 전에 확인 연락 부탁드립니다.",
                "신선 전복 세트",
                "READY_FOR_DELIVERY"
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 주문에 이미 deliveryId 가 연결되어 있음")
    void create_delivery_fail_order_already_has_delivery_id() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                supplierCompanyId,
                receiverCompanyId,
                deliveryId,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "배송 전에 확인 연락 부탁드립니다.",
                "신선 전복 세트",
                "READY_FOR_DELIVERY"
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 허브가 존재하지 않음")
    void create_delivery_fail_hub_not_found() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, originHubId));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(activeReceiverCompanyResponse(receiverCompanyId, destinationHubId));
        given(hubClient.existsHub(eq(originHubId), any()))
            .willReturn(hubExistsResponse(originHubId, false));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 최적 경로가 비어 있음")
    void create_delivery_fail_empty_route() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, originHubId));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(activeReceiverCompanyResponse(receiverCompanyId, destinationHubId));
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
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

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - hubClient exists 호출 중 예외가 발생하면 HUB_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_hub_client_exists_throws_exception() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, originHubId));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(activeReceiverCompanyResponse(receiverCompanyId, destinationHubId));
        given(hubClient.existsHub(eq(originHubId), any()))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - companyClient 호출 중 예외가 발생하면 COMPANY_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_company_client_throws_exception() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));
        given(companyClient.getCompany(supplierCompanyId))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - orderClient 호출 중 예외가 발생하면 ORDER_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_order_client_throws_exception() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - hubClient optimal route 호출 중 예외가 발생하면 HUB_SERVICE_UNAVAILABLE 로 변환")
    void create_delivery_fail_when_hub_client_route_throws_exception() {
        UUID orderId = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        given(orderClient.getOrder(orderId))
            .willReturn(readyForDeliveryOrderResponse(orderId, supplierCompanyId, receiverCompanyId));
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, originHubId));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(activeReceiverCompanyResponse(receiverCompanyId, destinationHubId));
        given(hubClient.existsHub(eq(originHubId), any())).willReturn(hubExistsResponse(originHubId, true));
        given(hubClient.existsHub(eq(destinationHubId), any())).willReturn(hubExistsResponse(destinationHubId, true));
        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(hubClient.getOptimalRoute(eq(originHubId), eq(destinationHubId), any()))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 상태 변경 성공 - WAITING_AT_HUB 에서 MOVING_BETWEEN_HUBS 로 변경")
    void change_delivery_status_success() {
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();
        ChangeDeliveryStatusRequest request = new ChangeDeliveryStatusRequest(
            DeliveryStatus.MOVING_BETWEEN_HUBS
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        DeliveryResponse response = deliveryService.changeDeliveryStatus(deliveryId, request);

        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.MOVING_BETWEEN_HUBS);
        assertThat(delivery.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("배송 상태 변경 실패 - 허용되지 않은 상태 전이")
    void change_delivery_status_fail_invalid_transition() {
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();
        ChangeDeliveryStatusRequest request = new ChangeDeliveryStatusRequest(
            DeliveryStatus.DELIVERED
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.changeDeliveryStatus(deliveryId, request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_STATUS_CHANGE_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("배송 취소 성공 - WAITING_AT_HUB 상태에서만 가능")
    void cancel_delivery_success() {
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
            .willReturn(List.of());

        DeliveryResponse response = deliveryService.cancelDelivery(deliveryId);

        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("배송 취소 실패 - WAITING_AT_HUB 이외 상태에서는 불가")
    void cancel_delivery_fail_when_not_waiting() {
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = createDelivery();
        delivery.updateStatus(DeliveryStatus.MOVING_BETWEEN_HUBS);

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(deliveryId))
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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createCompanyDeliveryManager(destinationHubId);
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

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
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(managerId);

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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createHubDeliveryManager();
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        DeliveryManager manager = createCompanyDeliveryManager(anotherHubId);
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

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
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );
        delivery.cancel();

        DeliveryManager manager = createCompanyDeliveryManager(destinationHubId);
        AssignCompanyDeliveryManagerRequest request = new AssignCompanyDeliveryManagerRequest(manager.getId());

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

        deliveryService.deleteDelivery(deliveryId);

        assertThat(delivery.getDeletedAt()).isNotNull();
        assertThat(delivery.getDeletedBy()).isEqualTo(SYSTEM_ACTOR_ID);
        assertThat(routeLog.getDeletedAt()).isNotNull();
        assertThat(routeLog.getDeletedBy()).isEqualTo(SYSTEM_ACTOR_ID);
    }

    @Test
    @DisplayName("AI 배송 정보 조회 성공 - 주문, 허브 정보를 조합해 응답한다")
    void get_ai_delivery_info_success() {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID originHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        Delivery delivery = Delivery.create(
            orderId,
            originHubId,
            destinationHubId,
            receiverCompanyId,
            "부산광역시 해운대구 우동",
            "101호",
            "홍길동",
            "U12345678",
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                receiverCompanyId,
                deliveryId,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "선물용이라 반드시 내일 오후 2시 전에는 도착해야 합니다. 안전 배송 부탁드려요.",
                "신선 전복 세트",
                "READY_FOR_DELIVERY"
            ));
        given(hubClient.getHub(eq(originHubId), any()))
            .willReturn(new HubInternalResponse(
                originHubId,
                "서울 허브",
                "서울특별시 송파구 송파동"
            ));
        given(hubClient.getHub(eq(destinationHubId), any()))
            .willReturn(new HubInternalResponse(
                destinationHubId,
                "부산 허브",
                "부산광역시 해운대구 우동"
            ));

        AiDeliveryResponse response = deliveryService.getAiDeliveryInfo(deliveryId);

        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getOriginHubId()).isEqualTo(originHubId);
        assertThat(response.getDestinationHubId()).isEqualTo(destinationHubId);
        assertThat(response.getOriginHubName()).isEqualTo("서울 허브");
        assertThat(response.getDestinationHubName()).isEqualTo("부산 허브");
        assertThat(response.getOriginAddress()).isEqualTo("서울특별시 송파구 송파동");
        assertThat(response.getDestinationAddress()).isEqualTo("부산광역시 해운대구 우동");
        assertThat(response.getProductName()).isEqualTo("신선 전복 세트");
        assertThat(response.getOrderRequestDetails())
            .isEqualTo("선물용이라 반드시 내일 오후 2시 전에는 도착해야 합니다. 안전 배송 부탁드려요.");
        assertThat(response.getReceiverSlackId()).isEqualTo("U12345678");
    }

    @Test
    @DisplayName("AI 배송 정보 조회 실패 - 배송이 없으면 DELIVERY_NOT_FOUND")
    void get_ai_delivery_info_fail_delivery_not_found() {
        UUID deliveryId = UUID.randomUUID();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getAiDeliveryInfo(deliveryId))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("AI 배송 정보 조회 실패 - 주문이 없으면 ORDER_NOT_FOUND")
    void get_ai_delivery_info_fail_order_not_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(orderClient.getOrder(delivery.getOrderId()))
            .willThrow(feignNotFoundException());

        assertThatThrownBy(() -> deliveryService.getAiDeliveryInfo(deliveryId))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("AI 배송 정보 조회 실패 - 주문 서비스 예외면 ORDER_SERVICE_UNAVAILABLE")
    void get_ai_delivery_info_fail_order_service_unavailable() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(orderClient.getOrder(delivery.getOrderId()))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.getAiDeliveryInfo(deliveryId))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("AI 배송 정보 조회 실패 - 허브가 없으면 HUB_NOT_FOUND")
    void get_ai_delivery_info_fail_hub_not_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(orderClient.getOrder(delivery.getOrderId()))
            .willReturn(new OrderInternalResponse(
                delivery.getOrderId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                delivery.getReceiverCompanyId(),
                deliveryId,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "요청사항",
                "신선 전복 세트",
                "READY_FOR_DELIVERY"
            ));
        given(hubClient.getHub(eq(delivery.getOriginHubId()), any()))
            .willThrow(feignNotFoundException());

        assertThatThrownBy(() -> deliveryService.getAiDeliveryInfo(deliveryId))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("AI 배송 정보 조회 실패 - 허브 서비스 예외면 HUB_SERVICE_UNAVAILABLE")
    void get_ai_delivery_info_fail_hub_service_unavailable() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = createDelivery();

        given(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).willReturn(Optional.of(delivery));
        given(orderClient.getOrder(delivery.getOrderId()))
            .willReturn(new OrderInternalResponse(
                delivery.getOrderId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                delivery.getReceiverCompanyId(),
                deliveryId,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "요청사항",
                "신선 전복 세트",
                "READY_FOR_DELIVERY"
            ));
        given(hubClient.getHub(eq(delivery.getOriginHubId()), any()))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.getAiDeliveryInfo(deliveryId))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
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

    private HubExistsResponse hubExistsResponse(UUID hubId, boolean exists) {
        return new HubExistsResponse(
            true,
            new HubExistsResponse.HubExistsData(hubId, exists),
            "SUCCESS",
            "요청이 성공했습니다."
        );
    }

    private CompanyInternalResponse activeSupplierCompanyResponse(UUID companyId, UUID hubId) {
        return new CompanyInternalResponse(
            companyId,
            "공급 업체",
            "PRODUCER",
            hubId,
            "서울시 송파구 올림픽로 1",
            "201호",
            "공급담당자",
            "U_SUPPLIER",
            true
        );
    }

    private CompanyInternalResponse activeReceiverCompanyResponse(UUID companyId, UUID hubId) {
        return new CompanyInternalResponse(
            companyId,
            "수령 업체",
            "RECEIVER",
            hubId,
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            true
        );
    }

    private OrderInternalResponse readyForDeliveryOrderResponse(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId
    ) {
        return new OrderInternalResponse(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            null,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "배송 전에 확인 연락 부탁드립니다.",
            "신선 전복 세트",
            "READY_FOR_DELIVERY"
        );
    }

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

    private FeignException feignNotFoundException() {
        return FeignException.errorStatus(
            "test",
            feign.Response.builder()
                .status(404)
                .reason("Not Found")
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
