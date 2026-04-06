package com.team.deliveryservice.delivery.application.service;

import com.team.deliveryservice.delivery.domain.*;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerRepository;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryManagerAutoAssignServiceImpl implements DeliveryManagerAutoAssignService {

    private static final List<DeliveryStatus> INACTIVE_DELIVERY_STATUSES =
        List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED);

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryRouteLogRepository deliveryRouteLogRepository;
    private final DeliveryRepository deliveryRepository;

    @Override
    public void autoAssign(Delivery delivery) {
        autoAssignHubManagers(delivery);
        autoAssignCompanyManager(delivery);
    }

    private void autoAssignHubManagers(Delivery delivery) {
        List<DeliveryRouteLog> routeLogs =
            deliveryRouteLogRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(delivery.getId());

        for (DeliveryRouteLog routeLog : routeLogs) {
            List<DeliveryManager> candidates =
                deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                    DeliveryManagerType.HUB_DELIVERY_MANAGER,
                    routeLog.getDepartureHubId()
                );

            DeliveryManager selected = candidates.stream()
                .min(
                    Comparator
                        .comparingLong(this::getActiveHubAssignmentCount)
                        .thenComparingInt(DeliveryManager::getDeliverySequence)
                )
                .orElseThrow(() -> new ServiceException(
                    DeliveryErrorCode.HUB_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND
                ));

            routeLog.assignDeliveryManager(selected.getId());
        }
    }

    private void autoAssignCompanyManager(Delivery delivery) {
        List<DeliveryManager> candidates =
            deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
                delivery.getDestinationHubId()
            );

        DeliveryManager selected = candidates.stream()
            .min(
                Comparator
                    .comparingLong(this::getActiveCompanyAssignmentCount)
                    .thenComparingInt(DeliveryManager::getDeliverySequence)
            )
            .orElseThrow(() -> new ServiceException(
                DeliveryErrorCode.COMPANY_DELIVERY_MANAGER_CANDIDATE_NOT_FOUND
            ));

        delivery.assignCompanyDeliveryManager(selected.getId());
    }

    private static final List<DeliveryRouteStatus> INACTIVE_ROUTE_STATUSES =
        List.of(DeliveryRouteStatus.DELIVERED, DeliveryRouteStatus.CANCELLED);

    private long getActiveHubAssignmentCount(DeliveryManager manager) {
        UUID managerId = manager.getId();
        return deliveryRouteLogRepository.countByDeliveryManagerIdAndDeletedAtIsNullAndRouteStatusNotIn(
            managerId,
            INACTIVE_ROUTE_STATUSES
        );
    }

    private long getActiveCompanyAssignmentCount(DeliveryManager manager) {
        UUID managerId = manager.getId();
        return deliveryRepository.countByCompanyDeliveryManagerIdAndDeliveryStatusNotInAndDeletedAtIsNull(
            managerId,
            INACTIVE_DELIVERY_STATUSES
        );
    }
}
