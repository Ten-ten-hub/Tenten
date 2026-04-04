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

    /**
     * 첫 배송담당자 생성 시 sequence 시작값
     */
    private static final int INITIAL_SEQUENCE = 0;

    /**
     * 동시성 충돌이 발생했을 때 최대 재시도 횟수
     * - 기존 행이 있는 경우: 비관적 락으로 대부분 해결
     * - 첫 생성(empty result) 구간은 unique 제약 충돌 가능성이 있어 재시도로 보완
     */
    private static final int MAX_SEQUENCE_RETRY_COUNT = 3;

    private final DeliveryManagerRepository deliveryManagerRepository;

    @Override
    @Transactional
    public DeliveryManagerResponse createDeliveryManager(CreateDeliveryManagerRequest request, CurrentUser currentUser) {
        validateCreatePermission(request, currentUser);

        // 배송담당자 생성 시 sequence 충돌 가능성을 고려하여 재시도
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

                // saveAndFlush 로 DB 제약 조건 충돌을 즉시 감지
                DeliveryManager saved = deliveryManagerRepository.saveAndFlush(deliveryManager);
                return DeliveryManagerResponse.from(saved);

            } catch (IllegalArgumentException e) {
                // 도메인 검증 실패(타입/허브 조합 등)
                throw new ServiceException(DeliveryErrorCode.COMMON_INVALID_INPUT);

            } catch (DataIntegrityViolationException e) {
                // 첫 생성 구간 등에서 동시 생성이 겹치면 unique 충돌 가능
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

    /**
     * 활성 배송담당자 조회
     */
    private DeliveryManager getActiveDeliveryManager(UUID deliveryManagerId) {
        return deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId)
            .orElseThrow(() -> new ServiceException(DeliveryErrorCode.DELIVERY_MANAGER_NOT_FOUND));
    }

    /**
     * 타입/허브 조합에 맞는 다음 deliverySequence 조회
     * - 기존 행이 있으면 PESSIMISTIC_WRITE 로 마지막 행을 잠그고 +1
     * - 기존 행이 없으면 0부터 시작
     */
    private int getNextSequenceWithLock(DeliveryManagerType type, UUID hubId) {
        if (type == DeliveryManagerType.HUB_DELIVERY_MANAGER) {
            return deliveryManagerRepository
                .findTopByTypeAndDeletedAtIsNullOrderByDeliverySequenceDesc(type)
                .map(manager -> manager.getDeliverySequence() + 1)
                .orElse(INITIAL_SEQUENCE);
        }

        return deliveryManagerRepository
            .findTopByTypeAndHubIdAndDeletedAtIsNullOrderByDeliverySequenceDesc(type, hubId)
            .map(manager -> manager.getDeliverySequence() + 1)
            .orElse(INITIAL_SEQUENCE);
    }

    /**
     * sequence 관련 unique 충돌 여부 판별
     * - DB 제약 이름이 정확히 다를 수 있어 메시지를 넓게 확인
     * - "delivery_sequence", "uk_p_delivery_manager" 등을 기준으로 판별
     */
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

    /**
     * 배송담당자 생성 권한 검증
     * - MASTER_ADMIN: 전체 생성 가능
     * - HUB_ADMIN: 본인 허브의 업체 배송 담당자만 생성 가능
     */
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

    /**
     * 배송담당자 조회 권한 검증
     */
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

    /**
     * 배송담당자 수정 권한 검증
     */
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

    /**
     * 배송담당자 삭제 권한 검증
     */
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
