package com.team.deliveryservice.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.team.deliveryservice.delivery.application.dto.request.AssignCompanyDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.AssignHubDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.ChangeDeliveryStatusRequest;
import com.team.deliveryservice.delivery.application.dto.request.CreateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.response.AiDeliveryResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.delivery.application.service.DeliveryManagerAutoAssignService;
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
import com.team.deliveryservice.infrastructure.client.dto.OptimalRouteResponseWrapper;
import com.team.deliveryservice.infrastructure.client.dto.OrderInternalResponse;
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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    private static final UUID SYSTEM_ACTOR_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final UUID FIXED_ORIGIN_HUB_ID =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    private static final UUID FIXED_DESTINATION_HUB_ID =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440013");

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

    @Mock
    private DeliveryManagerAutoAssignService deliveryManagerAutoAssignService;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private static final String INTERNAL_HEADER = "true";

    /**
     * 배송 생성 전 검증 단계에 필요한 의존성(주문, 업체, 허브 존재 여부)을 설정하는 헬퍼 메서드
     */
    private void givenCommonCreateDependencies(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId,
        UUID originHubId,
        UUID destinationHubId
    ) {
        // 1. 주문 서비스: 배송 대기 중인 주문 정보 반환
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        
        // 2. 업체 서비스: 활성화된 공급/수령 업체 정보 반환
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, originHubId));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(activeReceiverCompanyResponse(receiverCompanyId, destinationHubId));
        
        // 3. 허브 서비스: 각 허브의 존재 여부 확인 (내부 호출 헤더 검증 포함)
        given(hubClient.existsHub(eq(originHubId), eq(INTERNAL_HEADER)))
            .willReturn(new HubExistsResponse(true, new HubExistsResponse.HubExistsData(originHubId, true), "200", "SUCCESS"));
        given(hubClient.existsHub(eq(destinationHubId), eq(INTERNAL_HEADER)))
            .willReturn(new HubExistsResponse(true, new HubExistsResponse.HubExistsData(destinationHubId, true), "200", "SUCCESS"));
    }

    /**
     * 배송 저장 후 경로 생성을 위한 최적 경로 의존성을 설정하는 헬퍼 메서드
     */
    private void givenOptimalRoute(UUID originHubId, UUID destinationHubId) {
        given(hubClient.getOptimalRoute(eq(originHubId), eq(destinationHubId), eq(INTERNAL_HEADER)))
            .willReturn(new OptimalRouteResponseWrapper(200, "SUCCESS", new OptimalRouteResponseWrapper.OptimalRouteResponse(
                originHubId,
                destinationHubId,
                30,
                10.0,
                List.of(new OptimalRouteResponseWrapper.RoutePathResponse(1, originHubId, destinationHubId, 30, 10.0))
            )));
    }

    @Test
    @DisplayName("배송 생성 성공 - 활성 업체 정보로 배송 생성 및 경로 로그 생성 확인")
    void create_delivery_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderedBy = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            orderedBy,
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "문 앞에 놓아주세요"
        );

        // 중복 배송 없음 설정
        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);

        // 모든 유효 의존성 설정 (헬퍼 사용)
        givenCommonCreateDependencies(
            orderId,
            supplierCompanyId,
            receiverCompanyId,
            FIXED_ORIGIN_HUB_ID,
            FIXED_DESTINATION_HUB_ID
        );
        givenOptimalRoute(FIXED_ORIGIN_HUB_ID, FIXED_DESTINATION_HUB_ID);

        // 배송 저장 모킹
        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // 생성된 경로 로그 조회 모킹
        DeliveryRouteLog mockRouteLog = DeliveryRouteLog.create(
            UUID.randomUUID(),
            1,
            FIXED_ORIGIN_HUB_ID,
            FIXED_DESTINATION_HUB_ID,
            BigDecimal.valueOf(10.0),
            30,
            null
        );
        given(deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(any()))
            .willReturn(List.of(mockRouteLog));

        // when
        DeliveryResponse response = deliveryService.createDelivery(request);

        // then
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.receiverCompanyId()).isEqualTo(receiverCompanyId);
        assertThat(response.originHubId()).isEqualTo(FIXED_ORIGIN_HUB_ID);
        assertThat(response.destinationHubId()).isEqualTo(FIXED_DESTINATION_HUB_ID);
        assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.WAITING_AT_HUB);

        // 경로 로그 생성 확인
        assertThat(response.routeLogs()).hasSize(1);
        assertThat(response.routeLogs().get(0).sequenceNo()).isEqualTo(1);

        // 자동 배정 서비스 호출 검증
        verify(deliveryManagerAutoAssignService).autoAssign(any(Delivery.class));
    }

    @Test
    @DisplayName("배송 생성 실패 - 자동 배정 가능한 허브 배송 담당자가 없으면 예외 발생")
    void create_delivery_fail_when_no_hub_delivery_manager_candidate() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderedBy = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            orderedBy,
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "문 앞에 놓아주세요"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        givenCommonCreateDependencies(
            orderId,
            supplierCompanyId,
            receiverCompanyId,
            FIXED_ORIGIN_HUB_ID,
            FIXED_DESTINATION_HUB_ID
        );
        givenOptimalRoute(FIXED_ORIGIN_HUB_ID, FIXED_DESTINATION_HUB_ID);

        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        willThrow(new ServiceException(DeliveryErrorCode.HUB_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND))
            .given(deliveryManagerAutoAssignService)
            .autoAssign(any(Delivery.class));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 자동 배정 가능한 업체 배송 담당자가 없으면 예외 발생")
    void create_delivery_fail_when_no_company_delivery_manager_candidate() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderedBy = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            orderedBy,
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "문 앞에 놓아주세요"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        givenCommonCreateDependencies(
            orderId,
            supplierCompanyId,
            receiverCompanyId,
            FIXED_ORIGIN_HUB_ID,
            FIXED_DESTINATION_HUB_ID
        );
        givenOptimalRoute(FIXED_ORIGIN_HUB_ID, FIXED_DESTINATION_HUB_ID);

        given(deliveryRepository.save(any(Delivery.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        willThrow(new ServiceException(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND))
            .given(deliveryManagerAutoAssignService)
            .autoAssign(any(Delivery.class));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND.getMessage());
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
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
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

        CreateDeliveryRequest request = new CreateDeliveryRequest(
            orderId,
            UUID.randomUUID(),
            supplierCompanyId,
            receiverCompanyId,
            LocalDateTime.of(2026, 4, 1, 18, 0),
            "요청사항"
        );

        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, UUID.randomUUID()));
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
    @DisplayName("배송 생성 실패 - 공급 업체의 허브 ID가 없으면 COMMON_INVALID_INPUT")
    void create_delivery_fail_supplier_hub_id_null() {
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
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(new CompanyInternalResponse(
                supplierCompanyId,
                "공급 업체",
                "PRODUCER",
                null,
                "서울시 송파구 올림픽로 1",
                "201호",
                "공급담당자",
                "U_SUPPLIER",
                true
            ));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(activeReceiverCompanyResponse(receiverCompanyId, UUID.randomUUID()));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 수령 업체의 허브 ID가 없으면 COMMON_INVALID_INPUT")
    void create_delivery_fail_receiver_hub_id_null() {
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
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, UUID.randomUUID()));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(new CompanyInternalResponse(
                receiverCompanyId,
                "수령 업체",
                "RECEIVER",
                null,
                "서울시 강남구 테헤란로 123",
                "101호",
                "홍길동",
                "U12345678",
                true
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 수령 업체 주소가 비어 있으면 COMMON_INVALID_INPUT")
    void create_delivery_fail_receiver_address_blank() {
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
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, UUID.randomUUID()));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(new CompanyInternalResponse(
                receiverCompanyId,
                "수령 업체",
                "RECEIVER",
                UUID.randomUUID(),
                "",
                "101호",
                "홍길동",
                "U12345678",
                true
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 수령 업체 담당자명이 비어 있으면 COMMON_INVALID_INPUT")
    void create_delivery_fail_receiver_contact_name_blank() {
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
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, UUID.randomUUID()));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(new CompanyInternalResponse(
                receiverCompanyId,
                "수령 업체",
                "RECEIVER",
                UUID.randomUUID(),
                "서울시 강남구 테헤란로 123",
                "101호",
                "",
                "U12345678",
                true
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 수령 업체 슬랙 ID가 비어 있으면 COMMON_INVALID_INPUT")
    void create_delivery_fail_receiver_contact_slack_blank() {
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
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        given(companyClient.getCompany(supplierCompanyId))
            .willReturn(activeSupplierCompanyResponse(supplierCompanyId, UUID.randomUUID()));
        given(companyClient.getCompany(receiverCompanyId))
            .willReturn(new CompanyInternalResponse(
                receiverCompanyId,
                "수령 업체",
                "RECEIVER",
                UUID.randomUUID(),
                "서울시 강남구 테헤란로 123",
                "101호",
                "홍길동",
                "",
                true
            ));

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
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
        givenReadyForDeliveryOrder(orderId, supplierCompanyId, receiverCompanyId);
        given(companyClient.getCompany(supplierCompanyId))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMPANY_SERVICE_UNAVAILABLE.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 이미 배송이 존재하는 주문(DB unique 제약 조건 위반)")
    void create_delivery_fail_unique_constraint() {
        // given
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

        // 중복 체크 통과 후 저장 시점에 충돌이 나는 시나리오
        given(deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).willReturn(false);
        givenCommonCreateDependencies(orderId, supplierCompanyId, receiverCompanyId, FIXED_ORIGIN_HUB_ID, FIXED_DESTINATION_HUB_ID);

        // DB Unique 제약 조건 위반 발생 시뮬레이션
        given(deliveryRepository.save(any(Delivery.class)))
            .willThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint uk_p_delivery_order_id_active"
            ));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("배송 생성 실패 - 데이터 무결성 위반 발생 시 공통 예외 반환")
    void create_delivery_fail_data_integrity() {
        // given
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
        givenCommonCreateDependencies(orderId, supplierCompanyId, receiverCompanyId, FIXED_ORIGIN_HUB_ID, FIXED_DESTINATION_HUB_ID);

        // 기타 데이터 무결성 오류 발생 시뮬레이션
        given(deliveryRepository.save(any(Delivery.class)))
            .willThrow(new DataIntegrityViolationException("other constraint"));

        // when & then
        assertThatThrownBy(() -> deliveryService.createDelivery(request))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.COMMON_INVALID_INPUT.getMessage());
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

        Delivery delivery = createDeliveryWithDestinationHub(destinationHubId);

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
        UUID destinationHubId = UUID.randomUUID();

        CurrentUser currentUser = new CurrentUser(
            UUID.randomUUID(),
            "HUB_ADMIN",
            destinationHubId,
            null
        );

        Delivery delivery = createDeliveryWithDestinationHub(destinationHubId);
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

        Delivery delivery = createDeliveryWithDestinationHub(destinationHubId);

        DeliveryManager manager = createHubDeliveryManager(destinationHubId);
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

        Delivery delivery = createDeliveryWithDestinationHub(destinationHubId);

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

        Delivery delivery = createDeliveryWithDestinationHub(destinationHubId);
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
        UUID hubId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        DeliveryRouteLog routeLog = DeliveryRouteLog.create(
            delivery.getId(),
            1,
            hubId, // departureHubId matches manager's hubId
            UUID.randomUUID(),
            BigDecimal.valueOf(12.5),
            30,
            null
        );

        DeliveryManager manager = createHubDeliveryManager(hubId);
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
        UUID hubId = UUID.randomUUID();
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

        DeliveryManager manager = createHubDeliveryManager(hubId);
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
        UUID hubId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);

        Delivery delivery = createDelivery();
        DeliveryRouteLog routeLog = DeliveryRouteLog.builder()
            .id(UUID.randomUUID())
            .deliveryId(delivery.getId())
            .sequenceNo(1)
            .departureHubId(hubId) // match manager's hubId
            .arrivalHubId(UUID.randomUUID())
            .expectedDistanceKm(BigDecimal.valueOf(12.5))
            .expectedDurationMinutes(30)
            .realDurationMinutes(30)
            .routeStatus(DeliveryRouteStatus.DELIVERED)
            .deliveryManagerId(null)
            .departedAt(LocalDateTime.now().minusHours(1))
            .arrivedAt(LocalDateTime.now())
            .build();

        DeliveryManager manager = createHubDeliveryManager(hubId);
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
    @DisplayName("AI 배송 정보 조회 성공 - 주문 및 각 허브의 최신 정보를 조합하여 응답")
    void get_ai_delivery_info_success() {
        // given
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
        
        // 주문 서비스 호출 모킹
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

        // 허브 서비스 호출 모킹 (내부 헤더 명시적 검증)
        given(hubClient.getHub(eq(originHubId), eq(INTERNAL_HEADER)))
            .willReturn(new HubInternalResponse(
                originHubId,
                "서울 허브",
                "서울특별시 송파구 송파동"
            ));
        given(hubClient.getHub(eq(destinationHubId), eq(INTERNAL_HEADER)))
            .willReturn(new HubInternalResponse(
                destinationHubId,
                "부산 허브",
                "부산광역시 해운대구 우동"
            ));

        // when
        AiDeliveryResponse response = deliveryService.getAiDeliveryInfo(deliveryId);

        // then
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
    @DisplayName("AI 배송 정보 조회 실패 - 출발 허브 정보가 없을 때 HUB_NOT_FOUND")
    void get_ai_delivery_info_fail_hub_not_found() {
        // given
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
            
        // 허브를 찾을 수 없는 시나리오 (내부 헤더 명시적 검증)
        given(hubClient.getHub(eq(delivery.getOriginHubId()), eq(INTERNAL_HEADER)))
            .willThrow(feignNotFoundException());

        // when & then
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
        given(hubClient.getHub(delivery.getOriginHubId(), "true"))
            .willThrow(feignBadRequestException());

        assertThatThrownBy(() -> deliveryService.getAiDeliveryInfo(deliveryId))
            .isInstanceOf(ServiceException.class)
            .hasMessage(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE.getMessage());
    }

    private void givenReadyForDeliveryOrder(UUID orderId, UUID supplierCompanyId, UUID receiverCompanyId) {
        given(orderClient.getOrder(orderId))
            .willReturn(new OrderInternalResponse(
                orderId,
                UUID.randomUUID(),
                supplierCompanyId,
                receiverCompanyId,
                null,
                LocalDateTime.of(2026, 4, 1, 18, 0),
                "요청사항",
                "테스트 상품",
                "READY_FOR_DELIVERY"
            ));
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

    private Delivery createDeliveryWithDestinationHub(UUID destinationHubId) {
        return Delivery.create(
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

    private DeliveryManager createHubDeliveryManager(UUID hubId) {
        return DeliveryManager.create(
            UUID.randomUUID(),
            hubId,
            "U123HUB",
            DeliveryManagerType.HUB_DELIVERY_MANAGER,
            1
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
