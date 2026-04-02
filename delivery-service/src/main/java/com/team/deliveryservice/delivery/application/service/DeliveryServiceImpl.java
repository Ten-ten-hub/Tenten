package com.team.deliveryservice.delivery.application.service;

import com.team.common.page.PageSizeUtils;
import com.team.deliveryservice.delivery.application.dto.request.AssignCompanyDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.AssignHubDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.ChangeDeliveryStatusRequest;
import com.team.deliveryservice.delivery.application.dto.request.CreateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.request.UpdateDeliveryRequest;
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

    private final DeliveryRepository deliveryRepository;
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryRouteLogRepository deliveryRouteLogRepository;

    @Override
    @Transactional
    public DeliveryResponse createDelivery(CreateDeliveryRequest request, CurrentUser currentUser) {
        if (deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
        }

        Delivery delivery = Delivery.create(
            request.orderId(),
            request.originHubId(),
            request.destinationHubId(),
            request.receiverCompanyId(),
            request.deliveryAddress(),
            request.deliveryAddressDetail(),
            request.recipientName(),
            request.recipientSlackId(),
            request.finalDispatchDeadlineAt()
        );

        try {
            Delivery savedDelivery = deliveryRepository.save(delivery);
            return DeliveryResponse.from(savedDelivery, List.of());
        } catch (DataIntegrityViolationException e) {
            if (isOrderIdUniqueViolation(e)) {
                throw new ServiceException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
            }
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }
    }

    @Override
    public DeliveryResponse getDelivery(UUID deliveryId, CurrentUser currentUser) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    public DeliveryPageResponse searchDeliveries(DeliverySearchCondition condition, CurrentUser currentUser) {
        int normalizedSize = PageSizeUtils.normalize(condition.size());

        var pageResult = deliveryRepository.search(condition, normalizedSize);

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
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

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
    public DeliveryResponse changeDeliveryStatus(
        UUID deliveryId,
        ChangeDeliveryStatusRequest request,
        CurrentUser currentUser
    ) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        try {
            delivery.updateStatus(request.deliveryStatus());
        } catch (IllegalStateException e) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_STATUS_CHANGE_NOT_ALLOWED);
        }

        return DeliveryResponse.from(delivery, getRouteLogs(deliveryId));
    }

    @Override
    @Transactional
    public DeliveryResponse cancelDelivery(UUID deliveryId, CurrentUser currentUser) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        try {
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
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        DeliveryManager manager = deliveryManagerRepository.findByIdAndDeletedAtIsNull(request.deliveryManagerId())
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        if (manager.getType() != DeliveryManagerType.COMPANY_DELIVERY_MANAGER) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_TYPE_INVALID);
        }

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
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        DeliveryRouteLog routeLog = getRouteLog(request.routeLogId());

        if (!routeLog.getDeliveryId().equals(delivery.getId())) {
            throw new ServiceException(DeliveryErrorCode.DELIVERY_ROUTE_MANAGER_ASSIGN_NOT_ALLOWED);
        }

        DeliveryManager manager = deliveryManagerRepository.findByIdAndDeletedAtIsNull(request.deliveryManagerId())
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));

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
    public void deleteDelivery(UUID deliveryId, CurrentUser currentUser) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        delivery.softDelete(currentUser.userId());

        List<DeliveryRouteLog> routeLogs =
            deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId);

        routeLogs.forEach(routeLog -> routeLog.softDelete(currentUser.userId()));
    }

    private List<DeliveryRouteLogResponse> getRouteLogs(UUID deliveryId) {
        return deliveryRouteLogRepository
            .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId)
            .stream()
            .map(DeliveryRouteLogResponse::from)
            .toList();
    }

    private DeliveryRouteLog getRouteLog(UUID routeLogId) {
        return deliveryRouteLogRepository.findByIdAndDeletedAtIsNull(routeLogId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_ROUTE_LOG_NOT_FOUND));
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
}
