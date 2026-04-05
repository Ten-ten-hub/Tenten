package com.team.deliveryservice.delivery.application.service;

import com.team.common.page.PageSizeUtils;
import com.team.deliveryservice.delivery.application.dto.request.AssignCompanyDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.AssignHubDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.ChangeDeliveryStatusRequest;
import com.team.deliveryservice.delivery.application.dto.request.CreateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.request.UpdateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.response.AiDeliveryResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryPageResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryRouteLogResponse;
import com.team.deliveryservice.delivery.application.search.DeliverySearchCondition;
import com.team.deliveryservice.delivery.domain.Delivery;
import com.team.deliveryservice.delivery.domain.DeliveryRepository;
import com.team.deliveryservice.delivery.domain.DeliveryRouteLog;
import com.team.deliveryservice.delivery.domain.DeliveryRouteLogRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private static final String DELIVERY_ORDER_ID_UNIQUE_CONSTRAINT = "uk_p_delivery_order_id_active";
    private static final String INTERNAL_REQUEST_HEADER = "true";
    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final DeliveryRepository deliveryRepository;
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryRouteLogRepository deliveryRouteLogRepository;
    private final HubClient hubClient;
    private final CompanyClient companyClient;
    private final OrderClient orderClient;

    @Override
    @Transactional
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {
        // 같은 주문에 대한 배송이 이미 있으면 생성 불가
        if (deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
        }

        // 주문 존재 여부 / 주문 상태 / 공급업체 / 수령업체 일치 여부 검증
        validateOrderExistsAndStatus(
            request.orderId(),
            request.supplierCompanyId(),
            request.receiverCompanyId()
        );

        // 공급 업체 / 수령 업체 조회
        CompanyInternalResponse supplierCompany = getActiveCompany(request.supplierCompanyId());
        CompanyInternalResponse receiverCompany = getActiveCompany(request.receiverCompanyId());

        // 배송 생성에 필요한 업체 정보 검증
        validateCompanyDeliveryInfo(supplierCompany, receiverCompany);

        // 업체 소속 허브 조회
        UUID originHubId = supplierCompany.hubId();
        UUID destinationHubId = receiverCompany.hubId();

        // 허브 존재 여부 검증
        validateHubExists(originHubId);
        validateHubExists(destinationHubId);

        // 수령 업체 정보 기준으로 배송 생성
        Delivery delivery = Delivery.create(
            request.orderId(),
            originHubId,
            destinationHubId,
            request.receiverCompanyId(),
            receiverCompany.address(),
            receiverCompany.addressDetail(),
            receiverCompany.contactName(),
            receiverCompany.contactSlackId(),
            request.deadlineAt()
        );

        try {
            Delivery savedDelivery = deliveryRepository.save(delivery);

            // 허브 최적 경로 조회 후 route log 생성
            createRouteLogs(savedDelivery);

            return DeliveryResponse.from(savedDelivery, getRouteLogs(savedDelivery.getId()));
        } catch (DataIntegrityViolationException e) {
            if (isOrderIdUniqueViolation(e)) {
                throw new ServiceException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
            }
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }
    }

    @Override
    public DeliveryResponse getDelivery(UUID deliveryId) {
        Delivery delivery = getDeliveryEntity(deliveryId);
        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    public DeliveryResponse getDelivery(UUID deliveryId, CurrentUser currentUser) {
        // TODO: currentUser 기준 조회 권한 검증 추가
        Delivery delivery = getDeliveryEntity(deliveryId);
        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    public DeliveryPageResponse searchDeliveries(DeliverySearchCondition condition, CurrentUser currentUser) {
        // TODO: currentUser 기준 조회 범위 제한 추가
        int normalizedSize = PageSizeUtils.normalize(condition.size());

        var pageResult = deliveryRepository.search(condition, normalizedSize);

        var deliveryIds = pageResult.getContent().stream()
            .map(Delivery::getId)
            .toList();

        // route log를 한 번에 조회해서 N+1 문제 방지
        final Map<UUID, List<DeliveryRouteLogResponse>> routeLogsByDeliveryId =
            deliveryIds.isEmpty()
                ? Map.of()
                : deliveryRouteLogRepository
                .findAllByDeliveryIdInAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryIds)
                .stream()
                .collect(Collectors.groupingBy(
                    DeliveryRouteLog::getDeliveryId,
                    LinkedHashMap::new,
                    Collectors.mapping(DeliveryRouteLogResponse::from, Collectors.toList())
                ));

        var responsePage = pageResult.map(delivery ->
            DeliveryResponse.from(
                delivery,
                routeLogsByDeliveryId.getOrDefault(delivery.getId(), List.of())
            )
        );

        return DeliveryPageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public DeliveryResponse updateDelivery(UUID deliveryId, UpdateDeliveryRequest request, CurrentUser currentUser) {
        // TODO: currentUser 기준 수정 권한 검증 추가
        Delivery delivery = getDeliveryEntity(deliveryId);

        // 배송지/수령인 정보 수정
        delivery.updateInfo(
            request.deliveryAddress(),
            request.deliveryAddressDetail(),
            request.recipientName(),
            request.recipientSlackId()
        );

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public DeliveryResponse changeDeliveryStatus(UUID deliveryId, ChangeDeliveryStatusRequest request) {
        Delivery delivery = getDeliveryEntity(deliveryId);

        try {
            // 도메인 상태 전이 규칙에 따라 배송 상태 변경
            delivery.updateStatus(request.deliveryStatus());
        } catch (IllegalStateException e) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_STATUS_CHANGE_NOT_ALLOWED);
        }

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public DeliveryResponse cancelDelivery(UUID deliveryId) {
        Delivery delivery = getDeliveryEntity(deliveryId);

        try {
            // 취소 가능한 상태일 때만 배송 취소
            delivery.cancel();
        } catch (IllegalStateException e) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_CANCEL_NOT_ALLOWED);
        }

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public DeliveryResponse assignCompanyDeliveryManager(
        UUID deliveryId,
        AssignCompanyDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        // TODO: currentUser 기준 권한 검증 추가
        Delivery delivery = getDeliveryEntity(deliveryId);

        DeliveryManager manager = deliveryManagerRepository.findByIdAndDeletedAtIsNull(request.deliveryManagerId())
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        // 업체 배송 담당자만 배정 가능
        if (manager.getType() != DeliveryManagerType.COMPANY_DELIVERY_MANAGER) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID);
        }

        // 업체 배송 담당자는 배송의 도착 허브 소속이어야 함
        if (manager.getHubId() == null || !manager.getHubId().equals(delivery.getDestinationHubId())) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_HUB_MISMATCH);
        }

        try {
            delivery.assignCompanyDeliveryManager(manager.getId());
        } catch (IllegalStateException e) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ASSIGN_NOT_ALLOWED);
        }

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public DeliveryResponse assignHubDeliveryManager(
        UUID deliveryId,
        AssignHubDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        // TODO: currentUser 기준 권한 검증 추가
        Delivery delivery = getDeliveryEntity(deliveryId);

        DeliveryRouteLog routeLog = getRouteLog(request.routeLogId());

        // 다른 배송의 route log에는 배정할 수 없음
        if (!routeLog.getDeliveryId().equals(delivery.getId())) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED);
        }

        DeliveryManager manager = deliveryManagerRepository.findByIdAndDeletedAtIsNull(request.deliveryManagerId())
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        // 허브 배송 담당자만 route log에 배정 가능
        if (manager.getType() != DeliveryManagerType.HUB_DELIVERY_MANAGER) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID);
        }

        try {
            routeLog.assignDeliveryManager(manager.getId());
        } catch (IllegalStateException e) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED);
        }

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public void deleteDelivery(UUID deliveryId) {
        Delivery delivery = getDeliveryEntity(deliveryId);
        delivery.softDelete(SYSTEM_ACTOR_ID);

        // 배송 삭제 시 연결된 route log도 함께 soft delete
        List<DeliveryRouteLog> routeLogs =
            deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId);

        routeLogs.forEach(routeLog -> routeLog.softDelete(SYSTEM_ACTOR_ID));
    }

    @Override
    @Transactional(readOnly = true)
    public AiDeliveryResponse getAiDeliveryInfo(UUID deliveryId) {
        Delivery delivery = getDeliveryEntity(deliveryId);

        OrderInternalResponse order;
        try {
            order = orderClient.getOrder(delivery.getOrderId());
            if (order == null || order.id() == null) {
                throw new ServiceException(DeliveryErrorCode.ORDER_NOT_FOUND);
            }
        } catch (FeignException.NotFound e) {
            throw new ServiceException(DeliveryErrorCode.ORDER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE);
        }

        HubInternalResponse originHub = getHubInfo(delivery.getOriginHubId());
        HubInternalResponse destinationHub = getHubInfo(delivery.getDestinationHubId());

        return AiDeliveryResponse.builder()
            .orderId(delivery.getOrderId())
            .originHubId(delivery.getOriginHubId())
            .destinationHubId(delivery.getDestinationHubId())
            .originHubName(originHub.name())
            .destinationHubName(destinationHub.name())
            .originAddress(originHub.address())
            .destinationAddress(destinationHub.address())
            .productName(order.productName())
            .orderRequestDetails(order.requestNote())
            .receiverSlackId(delivery.getRecipientSlackId())
            .build();
    }

    // 삭제되지 않은 배송 조회
    private Delivery getDeliveryEntity(UUID deliveryId) {
        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
    }

    // 삭제되지 않은 route log 조회
    private DeliveryRouteLog getRouteLog(UUID routeLogId) {
        return deliveryRouteLogRepository.findByIdAndDeletedAtIsNull(routeLogId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_ROUTE_LOG_NOT_FOUND));
    }

    // 배송 응답에 포함할 route log 목록 조회
    private List<DeliveryRouteLogResponse> getRouteLogs(UUID deliveryId) {
        return deliveryRouteLogRepository
            .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId)
            .stream()
            .map(DeliveryRouteLogResponse::from)
            .toList();
    }

    // 허브 서비스에 내부 요청 헤더를 넣어 존재 여부 확인
    private void validateHubExists(UUID hubId) {
        try {
            HubExistsResponse response = hubClient.existsHub(hubId, INTERNAL_REQUEST_HEADER);

            if (response == null || !response.exists()) {
                throw new ServiceException(DeliveryErrorCode.HUB_NOT_FOUND);
            }
        } catch (FeignException.NotFound e) {
            throw new ServiceException(DeliveryErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
        }
    }

    // 업체가 존재하고 활성 상태인지 확인
    private CompanyInternalResponse getActiveCompany(UUID companyId) {
        try {
            CompanyInternalResponse company = companyClient.getCompany(companyId);

            if (company == null || company.id() == null || !company.isActive()) {
                throw new ServiceException(DeliveryErrorCode.COMPANY_NOT_FOUND);
            }

            return company;
        } catch (FeignException.NotFound e) {
            throw new ServiceException(DeliveryErrorCode.COMPANY_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(DeliveryErrorCode.COMPANY_SERVICE_UNAVAILABLE);
        }
    }

    // 배송 생성에 필요한 업체 정보 검증
    private void validateCompanyDeliveryInfo(
        CompanyInternalResponse supplierCompany,
        CompanyInternalResponse receiverCompany
    ) {
        if (supplierCompany.hubId() == null || receiverCompany.hubId() == null) {
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }

        if (receiverCompany.address() == null || receiverCompany.address().isBlank()) {
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }

        if (receiverCompany.contactName() == null || receiverCompany.contactName().isBlank()) {
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }

        if (receiverCompany.contactSlackId() == null || receiverCompany.contactSlackId().isBlank()) {
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }
    }

    // 주문 존재 여부, 배송 생성 가능 상태, 공급 업체 / 수령 업체 일치 여부 검증
    private void validateOrderExistsAndStatus(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId
    ) {
        try {
            OrderInternalResponse order = orderClient.getOrder(orderId);

            if (order == null || order.id() == null) {
                throw new ServiceException(DeliveryErrorCode.ORDER_NOT_FOUND);
            }

            if (order.deliveryId() != null) {
                throw new ServiceException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
            }

            if (!supplierCompanyId.equals(order.supplierCompanyId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
            }

            if (!receiverCompanyId.equals(order.receiverCompanyId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
            }

            if (!"READY_FOR_DELIVERY".equals(order.orderStatus())) {
                throw new ServiceException(DeliveryErrorCode.DELIVERY_CREATE_NOT_ALLOWED);
            }
        } catch (FeignException.NotFound e) {
            throw new ServiceException(DeliveryErrorCode.ORDER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE);
        }
    }

    // 허브 최적 경로를 조회해 배송 경로 로그를 생성
    private void createRouteLogs(Delivery delivery) {
        try {
            OptimalRouteResponseWrapper routeResponse = hubClient.getOptimalRoute(
                delivery.getOriginHubId(),
                delivery.getDestinationHubId(),
                INTERNAL_REQUEST_HEADER
            );

            if (routeResponse == null
                || routeResponse.data() == null
                || routeResponse.data().routePathList() == null
                || routeResponse.data().routePathList().isEmpty()) {
                throw new ServiceException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
            }

            List<DeliveryRouteLog> routeLogs = routeResponse.data().routePathList().stream()
                .map(path -> {
                    if (path.sequence() == null
                        || path.departureHubId() == null
                        || path.arrivalHubId() == null
                        || path.distance() == null
                        || path.duration() == null) {
                        throw new ServiceException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
                    }

                    return DeliveryRouteLog.create(
                        delivery.getId(),
                        path.sequence(),
                        path.departureHubId(),
                        path.arrivalHubId(),
                        BigDecimal.valueOf(path.distance()),
                        path.duration(),
                        null
                    );
                })
                .toList();

            deliveryRouteLogRepository.saveAll(routeLogs);
        } catch (FeignException.NotFound e) {
            throw new ServiceException(DeliveryErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
        }
    }

    // DB unique 제약 위반 메시지에서 orderId 중복 여부 판별
    private boolean isOrderIdUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains(DELIVERY_ORDER_ID_UNIQUE_CONSTRAINT)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private HubInternalResponse getHubInfo(UUID hubId) {
        try {
            HubInternalResponse hub = hubClient.getHub(hubId, INTERNAL_REQUEST_HEADER);

            if (hub == null || hub.id() == null) {
                throw new ServiceException(DeliveryErrorCode.HUB_NOT_FOUND);
            }

            return hub;
        } catch (FeignException.NotFound e) {
            throw new ServiceException(DeliveryErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
        }
    }
}
