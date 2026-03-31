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
import org.springframework.context.ApplicationEventPublisher;
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

    @Transactional
    public void createAndSend(NotificationRequest dto) {
        String targetSlackId = dto.receiverSlackId();

        // 대상자 식별 예외 처리
        // ID가 없고 이메일이 있다면 이메일로 ID 조회
        if ((targetSlackId == null || targetSlackId.isEmpty()) && dto.email() != null) {
            targetSlackId = slackClient.findSlackIdByEmail(dto.email());
        }

        if (targetSlackId == null) {
            log.error("대상슬랙 ID를 찾을 수 없습니다. Email: {}", dto.email());
            throw new ServiceException(ErrorCode.NOTI_RECIPIENT_NOT_FOUND);
        }

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

    public Page<NotificationResponse> searchNotifications(NotificationSearchCondition condition, Pageable pageable) {
        Page<Notification> result;

        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            result = notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(
                condition.slackId(), condition.keyword(), pageable);
        } else {
            result = notificationRepository.findByReceiverSlackIdAndDeletedAtIsNull(
                condition.slackId(), pageable);
        }

//        // 결과가 비어있으면 404 예외 발생
//        if (result.isEmpty()) {
//            throw new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND);
//        }
//        return result.map(NotificationResponse::from);

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
