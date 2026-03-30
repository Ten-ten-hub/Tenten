package com.team.deliveryservice.application.delivery;

import com.team.deliveryservice.application.common.PageSizeUtils;
import com.team.deliveryservice.domain.delivery.Delivery;
import com.team.deliveryservice.domain.delivery.DeliveryRepository;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLog;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLogRepository;
import com.team.deliveryservice.presentation.common.CurrentUser;
import com.team.deliveryservice.presentation.common.ErrorCode;
import com.team.deliveryservice.presentation.common.ServiceException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteLogRepository deliveryRouteLogRepository;

    @Override
    @Transactional
    public DeliveryResponse createDelivery(CreateDeliveryRequest request, CurrentUser currentUser) {
        if (deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())) {
            throw new ServiceException(ErrorCode.DELIVERY_ALREADY_EXISTS);
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
            request.companyDeliveryManagerId(),
            request.finalDispatchDeadlineAt()
        );

        Delivery savedDelivery = deliveryRepository.save(delivery);

        return DeliveryResponse.from(savedDelivery, List.of());
    }

    @Override
    public DeliveryResponse getDelivery(UUID deliveryId, CurrentUser currentUser) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(ErrorCode.DELIVERY_NOT_FOUND));

        List<DeliveryRouteLogResponse> routeLogs = deliveryRouteLogRepository
            .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId)
            .stream()
            .map(DeliveryRouteLogResponse::from)
            .toList();

        return DeliveryResponse.from(delivery, routeLogs);
    }

    @Override
    public DeliveryPageResponse searchDeliveries(DeliverySearchCondition condition, CurrentUser currentUser) {
        int normalizedSize = PageSizeUtils.normalize(condition.size());

        var pageResult = deliveryRepository.search(condition, normalizedSize);

        var responsePage = pageResult.map(delivery -> {
            var routeLogs = deliveryRouteLogRepository
                .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(delivery.getId())
                .stream()
                .map(DeliveryRouteLogResponse::from)
                .toList();

            return DeliveryResponse.from(delivery, routeLogs);
        });

        return DeliveryPageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public DeliveryResponse updateDelivery(UUID deliveryId, UpdateDeliveryRequest request, CurrentUser currentUser) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.updateInfo(
            request.deliveryAddress(),
            request.deliveryAddressDetail(),
            request.recipientName(),
            request.recipientSlackId(),
            request.companyDeliveryManagerId()
        );

        List<DeliveryRouteLogResponse> routeLogs = deliveryRouteLogRepository
            .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId)
            .stream()
            .map(DeliveryRouteLogResponse::from)
            .collect(Collectors.toList());

        return DeliveryResponse.from(delivery, routeLogs);
    }

    @Override
    @Transactional
    public void deleteDelivery(UUID deliveryId, CurrentUser currentUser) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new ServiceException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.softDelete(currentUser.userId());

        List<DeliveryRouteLog> routeLogs =
            deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId);

        routeLogs.forEach(routeLog -> routeLog.softDelete(currentUser.userId()));
    }
}
