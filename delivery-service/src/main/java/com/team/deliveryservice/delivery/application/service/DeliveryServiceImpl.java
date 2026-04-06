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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    private final DeliveryManagerAutoAssignService deliveryManagerAutoAssignService;
    private final HubClient hubClient;
    private final CompanyClient companyClient;
    private final OrderClient orderClient;

    @Override
    @Transactional
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {
        if (deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())) {
            log.warn("[배송 생성] 이미 존재하는 배송 orderId={}", request.orderId());
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
        }

        validateOrderExistsAndStatus(
            request.orderId(),
            request.supplierCompanyId(),
            request.receiverCompanyId()
        );

        CompanyInternalResponse supplierCompany = getActiveCompany(request.supplierCompanyId());
        CompanyInternalResponse receiverCompany = getActiveCompany(request.receiverCompanyId());

        validateCompanyDeliveryInfo(supplierCompany, receiverCompany);

        UUID originHubId = supplierCompany.hubId();
        UUID destinationHubId = receiverCompany.hubId();

        validateHubExists(originHubId);
        validateHubExists(destinationHubId);

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
            createRouteLogs(savedDelivery);
            deliveryManagerAutoAssignService.autoAssign(savedDelivery);
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
        Delivery delivery = getDeliveryEntity(deliveryId);
        validateDeliveryReadPermission(delivery, currentUser);
        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    public DeliveryPageResponse searchDeliveries(DeliverySearchCondition condition, CurrentUser currentUser) {
        int normalizedSize = PageSizeUtils.normalize(condition.size());

        var pageResult = deliveryRepository.search(condition, currentUser, normalizedSize);
        var deliveryIds = pageResult.getContent().stream()
            .map(Delivery::getId)
            .toList();

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
        Delivery delivery = getDeliveryEntity(deliveryId);
        validateDeliveryUpdatePermission(delivery, currentUser);

        try {
            delivery.updateInfo(
                request.deliveryAddress(),
                request.deliveryAddressDetail(),
                request.recipientName(),
                request.recipientSlackId()
            );
        } catch (IllegalStateException e) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_UPDATE_NOT_ALLOWED);
        }

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public DeliveryResponse changeDeliveryStatus(UUID deliveryId, ChangeDeliveryStatusRequest request) {
        Delivery delivery = getDeliveryEntity(deliveryId);

        try {
            delivery.updateStatus(request.deliveryStatus());
        } catch (IllegalStateException e) {
            log.warn("[배송 상태 변경] 변경 불가 deliveryId={}, requestStatus={}", deliveryId, request.deliveryStatus(), e);
            throw new ServiceException(DeliveryErrorCode.DELIVERY_STATUS_CHANGE_NOT_ALLOWED);
        }

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public DeliveryResponse cancelDelivery(UUID deliveryId) {
        Delivery delivery = getDeliveryEntity(deliveryId);

        try {
            delivery.cancel();
        } catch (IllegalStateException e) {
            log.warn("[배송 취소] 취소 불가 상태 deliveryId={}", deliveryId, e);
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
        Delivery delivery = getDeliveryEntity(deliveryId);
        validateCompanyManagerAssignPermission(delivery, currentUser);

        DeliveryManager manager = deliveryManagerRepository.findByIdAndDeletedAtIsNull(request.deliveryManagerId())
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        if (manager.getType() != DeliveryManagerType.COMPANY_DELIVERY_MANAGER) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID);
        }

        if (!manager.getHubId().equals(delivery.getDestinationHubId())) {
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
        Delivery delivery = getDeliveryEntity(deliveryId);
        DeliveryRouteLog routeLog = getRouteLog(request.routeLogId());

        if (!routeLog.getDeliveryId().equals(delivery.getId())) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED);
        }

        validateHubManagerAssignPermission(routeLog, currentUser);

        DeliveryManager manager = deliveryManagerRepository.findByIdAndDeletedAtIsNull(request.deliveryManagerId())
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        if (manager.getType() != DeliveryManagerType.HUB_DELIVERY_MANAGER) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID);
        }

        if (!manager.getHubId().equals(routeLog.getDepartureHubId())) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_HUB_MISMATCH);
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

    private Delivery getDeliveryEntity(UUID deliveryId) {
        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
    }

    private DeliveryRouteLog getRouteLog(UUID routeLogId) {
        return deliveryRouteLogRepository.findByIdAndDeletedAtIsNull(routeLogId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_ROUTE_LOG_NOT_FOUND));
    }

    private List<DeliveryRouteLogResponse> getRouteLogs(UUID deliveryId) {
        return deliveryRouteLogRepository
            .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId)
            .stream()
            .map(DeliveryRouteLogResponse::from)
            .toList();
    }

    private void validateHubExists(UUID hubId) {
        try {
            HubExistsResponse response = hubClient.existsHub(hubId, INTERNAL_REQUEST_HEADER);

            boolean exists = response != null
                && response.data() != null
                && response.data().exists();

            if (!exists) {
                log.warn("[배송 생성] 허브 없음 hubId={}", hubId);
                throw new ServiceException(DeliveryErrorCode.HUB_NOT_FOUND);
            }
        } catch (FeignException.NotFound e) {
            log.warn("[배송 생성] 허브 없음 hubId={}", hubId, e);
            throw new ServiceException(DeliveryErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            log.error("[배송 생성] 허브 서비스 호출 실패 hubId={}, status={}", hubId, e.status(), e);
            throw new ServiceException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
        }
    }

    private CompanyInternalResponse getActiveCompany(UUID companyId) {
        try {
            CompanyInternalResponse company = companyClient.getCompany(companyId);

            if (company == null || company.id() == null || !company.isActive()) {
                log.warn("[배송 생성] 업체 없음 또는 비활성 companyId={}", companyId);
                throw new ServiceException(DeliveryErrorCode.COMPANY_NOT_FOUND);
            }

            return company;
        } catch (FeignException.NotFound e) {
            log.warn("[배송 생성] 업체 없음 companyId={}", companyId, e);
            throw new ServiceException(DeliveryErrorCode.COMPANY_NOT_FOUND);
        } catch (FeignException e) {
            log.error("[배송 생성] 업체 서비스 호출 실패 companyId={}, status={}", companyId, e.status(), e);
            throw new ServiceException(DeliveryErrorCode.COMPANY_SERVICE_UNAVAILABLE);
        }
    }

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

    private void validateDeliveryReadPermission(Delivery delivery, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
        }

        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin()) {
            if (currentUser.hubId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            boolean relatedHub = currentUser.hubId().equals(delivery.getOriginHubId())
                || currentUser.hubId().equals(delivery.getDestinationHubId());

            if (!relatedHub) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        if (currentUser.isCompanyManager()) {
            if (currentUser.companyId() == null || !currentUser.companyId().equals(delivery.getReceiverCompanyId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        if (currentUser.isCompanyDeliveryManager()) {
            if (currentUser.userId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            if (delivery.getCompanyDeliveryManagerId() == null
                || !currentUser.userId().equals(delivery.getCompanyDeliveryManagerId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        if (currentUser.isHubDeliveryManager()) {
            if (currentUser.userId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            boolean assigned = deliveryRouteLogRepository
                .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(delivery.getId())
                .stream()
                .anyMatch(routeLog -> currentUser.userId().equals(routeLog.getDeliveryManagerId()));

            if (!assigned) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }

    private void validateDeliveryUpdatePermission(Delivery delivery, CurrentUser currentUser) {
        validateDeliveryReadPermission(delivery, currentUser);

        if (currentUser.isCompanyManager()) {
            throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
        }
    }

    private void validateCompanyManagerAssignPermission(Delivery delivery, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
        }

        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin()) {
            if (currentUser.hubId() == null || !currentUser.hubId().equals(delivery.getDestinationHubId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }

    private void validateHubManagerAssignPermission(DeliveryRouteLog routeLog, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
        }

        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin()) {
            if (currentUser.hubId() == null || !currentUser.hubId().equals(routeLog.getDepartureHubId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }
}
