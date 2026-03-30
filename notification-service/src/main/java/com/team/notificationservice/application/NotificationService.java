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
    private final SlackClient slackClient;

    @Transactional
    public void createAndSend(NotificationRequest dto) {
        String targetSlackId = dto.receiverSlackId();

        // 1. 대상자 식별 예외 처리
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

        // 2. 외부 서비스(슬랙) 호출 예외 처리
        // 실제 발송
        try {
            boolean success = slackClient.sendDirectMessage(targetSlackId, notification.getMsgContent());

            if (success) {
                notification.markAsSuccess();
            } else {
                notification.markAsFailed();
            }
        } catch (Exception e) {
            log.error("슬랙 발송 중 시스템 오류 발생: {}", e.getMessage());
            notification.markAsFailed();
            throw new ServiceException(ErrorCode.NOTI_SLACK_API_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchNotifications(NotificationSearchCondition condition, Pageable pageable) {
        // 키워드가 있으면 포함 검색, 없으면 기존대로 전체 조회
        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            return notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(
                    condition.slackId(), condition.keyword(), pageable)
                .map(NotificationResponse::from);
        }

        return notificationRepository.findByReceiverSlackIdAndDeletedAtIsNull(
                condition.slackId(), pageable)
            .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID id) {
        // 3. 존재하지 않는 자원에 대한 구체적 예외 발생
        return notificationRepository.findByIdAndDeletedAtIsNull(id)
            .map(NotificationResponse::from)
            .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));
    }

    @Transactional
    public void deleteNotification(UUID id, String deletedBy) {
        // 4. 삭제 대상 부재 시 예외 발생
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));

        // 엔티티에 삭제 처리를 위임
        notification.delete(deletedBy);
    }
}
