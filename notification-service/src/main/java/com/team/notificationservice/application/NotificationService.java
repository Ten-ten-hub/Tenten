package com.team.notificationservice.application;

import com.team.common.Constants;
import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.presentation.NotificationResponse;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackClient slackClient;// Listener로 옮길 예정이지만, ID 조회 로직 때문에 유지
    private final NotificationSaver notificationSaver;
    private final StringRedisTemplate redisTemplate; // Redis 추가
    private final AiNotificationProcessor aiNotificationProcessor;

    /**
     * 알림 생성 및 전송 엔트리 포인트 네트워크 호출(Slack API)을 포함하므로 @Transactional을 붙이지 않음!
     */
    public void createAndSend(NotificationRequest dto, String userIdFromHeader) {
        // 1. 수신자 슬랙 ID 식별
        String targetSlackId = resolveTargetSlackId(dto);

        // 2. 수신자 UUID 결정
        UUID receiverUuid = parseUserId(userIdFromHeader);

        if (receiverUuid == null) {
            // 필요 시 테스트용 UUID를 직접 넣거나, null로 보내어 Saver에서 0000... 시스템 ID가 박히게 함
            log.debug("헤더값이 없어서 일단 시스템id로 진행 ");
        }

        // 3. 별도 트랜잭션 컴포넌트(Saver)를 통해 저장 및 이벤트 발행
        notificationSaver.saveAndPublish(dto, targetSlackId, receiverUuid);
    }

    /**
     * Kafka로부터 받은 메시지를 처리하여 슬랙 전송
     */
    @Transactional
    public void createWithAiAnalysis(AiNotificationRequest aiRequest, String msgType) {
        log.info(">>>> [DB 저장 및 슬랙 발송 시작] RefID: {}", aiRequest.refId());

        MsgType type;
        try {
            type = MsgType.valueOf(msgType);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("[INVALID MSG_TYPE] '{}'. OrderID: {}", msgType, aiRequest.orderId());
            type = MsgType.ORDER_ALERT; // 기본값 처리
        }

        Notification notification = Notification.builder()
            .receiverId(aiRequest.receiverId())
            .receiverSlackId(aiRequest.receiverSlackId())
            .orderId(aiRequest.orderId())
            .msgType(type)
            .msgContent(aiRequest.msgContent())
            .sendStatus(SendStatus.PENDING)
            .scheduledAt(aiRequest.scheduledAt())
            .refId(aiRequest.refId())
            .build();

        Notification saved = notificationRepository.save(notification);

        try {
            slackClient.sendDirectMessage(saved.getReceiverSlackId(), saved.getMsgContent());
            saved.markAsSentImmediately();
            log.info(">>>> [슬랙 발송 최종 성공] ID: {}, SlackID: {}", saved.getId(), saved.getReceiverSlackId());
        } catch (Exception e) {
            log.error(">>>> [슬랙 API 호출 에러] : {}", e.getMessage());
            saved.markAsFailed();
        }
        notificationRepository.save(saved);
    }

    /**
     * Kafka Consumer 설정
     */
    @Bean
    public Consumer<AiNotificationRequest> consumeAiNotification() {
        log.info("[CHECK] consumeAiNotification Bean Initialized");

        return request -> {
            log.info(">>>> [KAFKA 수신 성공] OrderID: {}, RefID: {}", request.orderId(), request.refId());

            try {
                String msgType = (request.msgType() != null) ? request.msgType() : "ORDER_ALERT";
                // 신규 프로세서 호출
                aiNotificationProcessor.processAiNotification(request, msgType);
            } catch (Exception e) {
                log.error(">>>> [KAFKA 처리 중 에러 발생 - 재시도 및 DLQ 처리 진행] : {}", e.getMessage(), e);
                // 에러를 던져야 Kafka Binder가 재시도(Retry) 및 DLQ 이동을 수행함
                throw e;
            }
        };
    }

    /**
     * 수신자 Slack ID 식별 로직
     */
    private String resolveTargetSlackId(NotificationRequest dto) {
        String targetSlackId = dto.receiverSlackId();

        // targetSlackId가 없거나 공백인 경우, 이메일이 유효하다면 Slack API 호출
        if ((targetSlackId == null || targetSlackId.isBlank()) && dto.email() != null && !dto.email().isBlank()) {
            // Redis 캐시 확인
            String cacheKey = "slack:email:" + dto.email();

            try {
                // Redis 조회 실패 시 예외를 잡아서 로그만 남기고 다음 단계(API 호출)로 진행 (Fail-open)
                targetSlackId = redisTemplate.opsForValue().get(cacheKey);
            } catch (Exception e) {
                log.warn("Redis 조회 실패 - Fail-open 전략에 따라 API 호출로 진행합니다: {}", e.getMessage());
            }

            if (targetSlackId == null) {
                targetSlackId = slackClient.findSlackIdByEmail(dto.email());
                if (targetSlackId != null) {
                    try {
                        // 성공 시 1일간 캐싱, 저장 실패 시에도 로그만 남기고 결과 반환
                        redisTemplate.opsForValue().set(cacheKey, targetSlackId, 1, TimeUnit.DAYS);
                    } catch (Exception e) {
                        log.warn("Redis 저장 실패 - 캐싱 없이 진행합니다: {}", e.getMessage());
                    }
                }
            }
        }

        // 최종 결과가 여전히 비어있거나 공백이면 예외 발생
        if (targetSlackId == null || targetSlackId.isBlank()) {
            throw new ServiceException(ErrorCode.NOTI_RECIPIENT_NOT_FOUND);
        }
        return targetSlackId;
    }

    private UUID parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format: {}", userId);
            return null;
        }
    }

    public Page<NotificationResponse> searchNotifications(NotificationSearchCondition condition, Pageable pageable) {
        Page<Notification> result;

        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            result = notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(
                condition.slackId(), condition.keyword(), pageable);
        } else {
            result = notificationRepository.findByReceiverSlackIdAndDeletedAtIsNull(
                condition.slackId(), pageable);
        }

        // 결과가 비어있어도 404를 던지지 않고 빈 페이지 반환 (200 OK 일관성 유지)
        return result.map(NotificationResponse::from);
    }

    public NotificationResponse getNotification(UUID id) {
        return notificationRepository.findByIdAndDeletedAtIsNull(id)
            .map(NotificationResponse::from)
            .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));
    }

    @Transactional
    public void deleteNotification(UUID id, String deletedBy) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));

        // deletedBy가 null/blank/SYSTEM인 경우 조건문으로 처리하고, 그 외의 경우 try-catch를 통해 UUID 파싱 실패 시 시스템 ID로 대체
        UUID adminUuid = parseAdminId(deletedBy);
        notification.delete(adminUuid);
        notificationRepository.save(notification);
    }

    private UUID parseAdminId(String deletedBy) {
        if (deletedBy == null || deletedBy.isBlank() || Constants.SYSTEM_USER_ID.equals(deletedBy)) {
            return Constants.SYSTEM_UUID;
        }
        try {
            return UUID.fromString(deletedBy);
        } catch (IllegalArgumentException e) {
            log.debug("deletedBy UUID 파싱 실패, 시스템 ID로 대체: {}", deletedBy);
            return Constants.SYSTEM_UUID;
        }
    }
}
