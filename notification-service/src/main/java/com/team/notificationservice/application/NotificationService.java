package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.presentation.NotificationResponse;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackClient slackClient;// Listener로 옮길 예정이지만, ID 조회 로직 때문에 유지
    private final ApplicationEventPublisher eventPublisher;

    private NotificationService self;

    // 자기 자신을 주입받아 프록시를 통한 트랜잭션 호출 보장
    @Autowired
    public void setSelf(@Lazy NotificationService self) {
        this.self = self;
    }

    /**
     * 알림 생성 및 전송 엔트리 포인트 네트워크 호출(Slack API)을 포함하므로 @Transactional을 붙이지 않음!
     */
    public void createAndSend(NotificationRequest dto) {
        // 1. 수신자 식별 (네트워크 호출 포함 가능성) - 트랜잭션 밖에서 수행
        String targetSlackId = resolveTargetSlackId(dto);

        // 2. 실제 DB 저장 및 이벤트 발행 - 별도 트랜잭션 메서드 호출
        self.saveAndPublish(dto, targetSlackId);
    }

    /**
     * 실제 DB 저장 및 이벤트를 발행하는 핵심 로직 쓰기 트랜잭션 범위를 최소화
     */
    @Transactional
    public void saveAndPublish(NotificationRequest dto, String targetSlackId) {
        Notification notification = Notification.builder()
            .receiverSlackId(targetSlackId) // 조회된 혹은 입력된 ID 저장
            .orderId(dto.orderId())
            .msgContent(dto.message())
            .msgType(dto.msgType())
            .sendStatus(SendStatus.PENDING)
            .build();

        notificationRepository.save(notification);

        // 슬랙 발송 로직 대신 이벤트를 던짐
        eventPublisher.publishEvent(new NotificationCreatedEvent(
            notification.getId(),
            targetSlackId,
            notification.getMsgContent()
        ));
    }

    /**
     * 수신자 Slack ID 식별 로직
     */
    private String resolveTargetSlackId(NotificationRequest dto) {
        String targetSlackId = dto.receiverSlackId();

        // targetSlackId가 없거나 공백인 경우, 이메일이 유효하다면 Slack API 호출
        if ((targetSlackId == null || targetSlackId.isBlank()) && dto.email() != null && !dto.email().isBlank()) {
            targetSlackId = slackClient.findSlackIdByEmail(dto.email());
        }

        // 최종 결과가 여전히 비어있거나 공백이면 예외 발생
        if (targetSlackId == null || targetSlackId.isBlank()) {
            // 개인정보 보안을 위해 이메일 마스킹 처리 후 로그 기록
            String maskedEmail = maskEmail(dto.email());
            log.error("대상 슬랙 ID를 찾을 수 없습니다. Email: {}, OrderId: {}", maskedEmail, dto.orderId());
            throw new ServiceException(ErrorCode.NOTI_RECIPIENT_NOT_FOUND);
        }
        return targetSlackId;
    }

    /**
     * 이메일 마스킹 처리 (개인정보 보호)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "UNKNOWN";
        }
        // 앞 두 글자만 남기고 나머지는 마스킹 (예: te***@domain.com)
        return email.replaceAll("(^[^@]{2}|(?!^)\\G)[^@]", "$1*");
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
        // 삭제 대상 부재 시 예외 발생
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));

        // 엔티티에 삭제 처리를 위임
        notification.delete(deletedBy);

        notificationRepository.save(notification); // 변경 사항 명시적 반영
    }
}
