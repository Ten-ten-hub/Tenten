package com.team.deliveryservice.deliverymanager.application.service;

import com.team.common.page.PageSizeUtils;
import com.team.deliveryservice.deliverymanager.application.dto.request.CreateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.request.UpdateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerPageResponse;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerResponse;
import com.team.deliveryservice.deliverymanager.application.search.DeliveryManagerSearchCondition;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManager;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerRepository;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.global.common.CurrentUser;
import com.team.deliveryservice.global.error.DeliveryErrorCode;
import com.team.deliveryservice.global.error.ServiceException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerServiceImpl implements DeliveryManagerService {

    private static final int INITIAL_SEQUENCE = 0;
    private static final int MAX_SEQUENCE_RETRY_COUNT = 3;

    private final DeliveryManagerRepository deliveryManagerRepository;

    @Override
    @Transactional
    public DeliveryManagerResponse createDeliveryManager(CreateDeliveryManagerRequest request, CurrentUser currentUser) {
        validateCreatePermission(request, currentUser);

        for (int attempt = 1; attempt <= MAX_SEQUENCE_RETRY_COUNT; attempt++) {
            try {
                int nextSequence = getNextSequenceWithLock(request.type(), request.hubId());

                DeliveryManager deliveryManager = DeliveryManager.create(
                    request.userId(),
                    request.hubId(),
                    request.slackId(),
                    request.type(),
                    nextSequence
                );

                DeliveryManager saved = deliveryManagerRepository.saveAndFlush(deliveryManager);
                return DeliveryManagerResponse.from(saved);

            } catch (IllegalArgumentException e) {
                throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);

            } catch (DataIntegrityViolationException e) {
                if (isSequenceConflict(e) && attempt < MAX_SEQUENCE_RETRY_COUNT) {
                    continue;
                }

                if (isSequenceConflict(e)) {
                    throw new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_SEQUENCE_CONFLICT);
                }

                throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
            }
        }

        throw new ServiceException(DeliveryErrorCode.COMMON_INTERNAL_ERROR);
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
        int size = PageSizeUtils.normalize(condition.size());
        int page = condition.page() == null || condition.page() < 0 ? 0 : condition.page();

        var pageable = PageRequest.of(page, size);
        var result = deliveryManagerRepository.search(condition, currentUser, pageable)
            .map(DeliveryManagerResponse::from);

        return DeliveryManagerPageResponse.from(result);
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

        try {
            deliveryManager.update(
                request.hubId(),
                request.slackId(),
                request.type()
            );
        } catch (IllegalArgumentException e) {
            throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);
        }

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

    private int getNextSequenceWithLock(DeliveryManagerType type, UUID hubId) {
        return deliveryManagerRepository
            .findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(type, hubId)
            .map(manager -> manager.getDeliverySequence() + 1)
            .orElse(INITIAL_SEQUENCE);
    }

    private boolean isSequenceConflict(DataIntegrityViolationException e) {
        Throwable cause = e;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("delivery_sequence")
                    || lowerMessage.contains("uk_p_delivery_manager")
                    || lowerMessage.contains("p_delivery_manager")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
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
        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin()) {
            if (currentUser.hubId() == null || !currentUser.hubId().equals(deliveryManager.getHubId())) {
                throw new ServiceException(DeliveryErrorCode.COMMON_ACCESS_DENIED);
            }
            return;
        }

        if (currentUser.isHubDeliveryManager() || currentUser.isCompanyDeliveryManager()) {
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
