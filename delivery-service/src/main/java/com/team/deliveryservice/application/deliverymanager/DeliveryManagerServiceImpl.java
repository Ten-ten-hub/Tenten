package com.team.deliveryservice.application.deliverymanager;

import com.team.common.page.PageSizeUtils;
import com.team.common.page.SortDirection;
import com.team.deliveryservice.domain.deliverymanager.DeliveryManager;
import com.team.deliveryservice.domain.deliverymanager.DeliveryManagerRepository;
import com.team.deliveryservice.domain.deliverymanager.DeliveryManagerType;
import com.team.deliveryservice.presentation.common.CurrentUser;
import com.team.deliveryservice.presentation.common.DeliveryErrorCode;
import com.team.deliveryservice.presentation.common.ServiceException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerServiceImpl implements DeliveryManagerService {

    private final DeliveryManagerRepository deliveryManagerRepository;

    @Override
    @Transactional
    public DeliveryManagerResponse createDeliveryManager(CreateDeliveryManagerRequest request, CurrentUser currentUser) {
        validateCreatePermission(request, currentUser);

        int nextSequence = getNextSequence(request.type(), request.hubId());

        DeliveryManager deliveryManager = DeliveryManager.create(
            request.userId(),
            request.hubId(),
            request.slackId(),
            request.type(),
            nextSequence
        );

        DeliveryManager saved = deliveryManagerRepository.save(deliveryManager);
        return DeliveryManagerResponse.from(saved);
    }

    @Override
    public DeliveryManagerResponse getDeliveryManager(UUID deliveryManagerId, CurrentUser currentUser) {
        DeliveryManager deliveryManager = getActiveDeliveryManager(deliveryManagerId);
        validateReadPermission(deliveryManager, currentUser);
        return DeliveryManagerResponse.from(deliveryManager);
    }

    @Override
    public DeliveryManagerPageResponse searchDeliveryManagers(
        DeliveryManagerSearchCondition condition,
        CurrentUser currentUser
    ) {
        List<DeliveryManager> managers = deliveryManagerRepository.findAllByDeletedAtIsNull();

        managers = applyRoleFilter(managers, currentUser);
        managers = applyConditionFilter(managers, condition);
        managers = applySort(managers, condition);

        int size = PageSizeUtils.normalize(condition.size());
        int page = condition.page() == null || condition.page() < 0 ? 0 : condition.page();

        int start = page * size;
        int end = Math.min(start + size, managers.size());

        List<DeliveryManagerResponse> content = start >= managers.size()
            ? List.of()
            : managers.subList(start, end).stream()
            .map(DeliveryManagerResponse::from)
            .toList();

        PageImpl<DeliveryManagerResponse> resultPage =
            new PageImpl<>(content, PageRequest.of(page, size), managers.size());

        return DeliveryManagerPageResponse.from(resultPage);
    }

    @Override
    @Transactional
    public DeliveryManagerResponse updateDeliveryManager(
        UUID deliveryManagerId,
        UpdateDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        DeliveryManager deliveryManager = getActiveDeliveryManager(deliveryManagerId);
        validateUpdatePermission(deliveryManager, request, currentUser);

        deliveryManager.update(
            request.hubId(),
            request.slackId(),
            request.type()
        );

        return DeliveryManagerResponse.from(deliveryManager);
    }

    @Override
    @Transactional
    public void deleteDeliveryManager(UUID deliveryManagerId, CurrentUser currentUser) {
        DeliveryManager deliveryManager = getActiveDeliveryManager(deliveryManagerId);
        validateDeletePermission(deliveryManager, currentUser);
        deliveryManager.softDelete(currentUser.userId());
    }

    private DeliveryManager getActiveDeliveryManager(UUID deliveryManagerId) {
        return deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));
    }

    private int getNextSequence(DeliveryManagerType type, UUID hubId) {
        if (type == DeliveryManagerType.HUB_DELIVERY_MANAGER) {
            return deliveryManagerRepository
                .findTopByTypeAndDeletedAtIsNullOrderByDeliverySequenceDesc(type)
                .map(manager -> manager.getDeliverySequence() + 1)
                .orElse(0);
        }

        return deliveryManagerRepository
            .findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(type, hubId)
            .map(manager -> manager.getDeliverySequence() + 1)
            .orElse(0);
    }

    private List<DeliveryManager> applyRoleFilter(List<DeliveryManager> managers, CurrentUser currentUser) {
        String role = currentUser.role();

        if ("MASTER_ADMIN".equals(role)) {
            return managers;
        }

        if ("HUB_ADMIN".equals(role)) {
            if (currentUser.hubId() == null) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return managers.stream()
                .filter(manager -> currentUser.hubId().equals(manager.getHubId()))
                .toList();
        }

        if ("HUB_DELIVERY_MANAGER".equals(role) || "COM_DELIVERY_MANAGER".equals(role)) {
            return managers.stream()
                .filter(manager -> manager.getId().equals(currentUser.userId()))
                .toList();
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }

    private List<DeliveryManager> applyConditionFilter(
        List<DeliveryManager> managers,
        DeliveryManagerSearchCondition condition
    ) {
        return managers.stream()
            .filter(manager -> condition.hubId() == null || condition.hubId().equals(manager.getHubId()))
            .filter(manager -> condition.type() == null || condition.type() == manager.getType())
            .toList();
    }

    private List<DeliveryManager> applySort(
        List<DeliveryManager> managers,
        DeliveryManagerSearchCondition condition
    ) {
        String sortBy = (condition.sortBy() == null || condition.sortBy().isBlank())
            ? "deliverySequence"
            : condition.sortBy();

        SortDirection direction = SortDirection.from(condition.direction());

        Comparator<DeliveryManager> comparator = switch (sortBy) {
            case "createdAt" -> Comparator.comparing(DeliveryManager::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(DeliveryManager::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "deliverySequence" -> Comparator.comparing(DeliveryManager::getDeliverySequence);
            default -> Comparator.comparing(DeliveryManager::getDeliverySequence);
        };

        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        return managers.stream()
            .sorted(comparator)
            .toList();
    }

    private void validateCreatePermission(CreateDeliveryManagerRequest request, CurrentUser currentUser) {
        String role = currentUser.role();

        if ("MASTER_ADMIN".equals(role)) {
            return;
        }

        if ("HUB_ADMIN".equals(role)) {
            if (request.type() != DeliveryManagerType.COMPANY_DELIVERY_MANAGER) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            if (currentUser.hubId() == null || !currentUser.hubId().equals(request.hubId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }

    private void validateReadPermission(DeliveryManager deliveryManager, CurrentUser currentUser) {
        String role = currentUser.role();

        if ("MASTER_ADMIN".equals(role)) {
            return;
        }

        if ("HUB_ADMIN".equals(role)) {
            if (currentUser.hubId() == null || !currentUser.hubId().equals(deliveryManager.getHubId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        if ("HUB_DELIVERY_MANAGER".equals(role) || "COM_DELIVERY_MANAGER".equals(role)) {
            if (!currentUser.userId().equals(deliveryManager.getId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }

    private void validateUpdatePermission(
        DeliveryManager deliveryManager,
        UpdateDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        String role = currentUser.role();

        if ("MASTER_ADMIN".equals(role)) {
            return;
        }

        if ("HUB_ADMIN".equals(role)) {
            if (deliveryManager.getType() != DeliveryManagerType.COMPANY_DELIVERY_MANAGER ||
                request.type() != DeliveryManagerType.COMPANY_DELIVERY_MANAGER) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            if (currentUser.hubId() == null ||
                !currentUser.hubId().equals(deliveryManager.getHubId()) ||
                !currentUser.hubId().equals(request.hubId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }

    private void validateDeletePermission(DeliveryManager deliveryManager, CurrentUser currentUser) {
        String role = currentUser.role();

        if ("MASTER_ADMIN".equals(role)) {
            return;
        }

        if ("HUB_ADMIN".equals(role)) {
            if (deliveryManager.getType() != DeliveryManagerType.COMPANY_DELIVERY_MANAGER) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }

            if (currentUser.hubId() == null || !currentUser.hubId().equals(deliveryManager.getHubId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
    }
}
