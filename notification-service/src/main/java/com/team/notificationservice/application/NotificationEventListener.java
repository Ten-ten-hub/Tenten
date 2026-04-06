package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.infrastructure.SlackSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SlackClient slackClient;
    private final NotificationRepository notificationRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationSavedEvent(NotificationSavedEvent event) {
        Notification notification = notificationRepository
            .findByIdAndDeletedAtIsNull(event.notificationId())
            .orElse(null);
        if (notification == null) {
            return;
        }

        if (notification.getSendStatus() != SendStatus.PENDING) {
            log.debug(">>>> [이미 처리된 알림] ID: {}, Status: {}", event.notificationId(), notification.getSendStatus());
            return;
        }

        try {
            // 1. 결과값(SlackSendResult) 캡처
            SlackSendResult result = slackClient.sendDirectMessage(event.receiverSlackId(), event.msgContent());

            // 2. 결과에 따른 분기 처리
            if (result == SlackSendResult.SUCCESS) {
                notification.markAsSentImmediately();
                log.info(">>>> [슬랙 발송 성공] ID: {}", event.notificationId());
            } else if (result == SlackSendResult.RETRYABLE_FAILURE) {
                log.warn(">>>> [슬랙 재시도 가능 실패] ID: {}", event.notificationId());
                notification.markAsFailed(); // 재시도 로직이 없다면 우선 실패 처리
            } else {
                log.error(">>>> [슬랙 발송 알 수 없는 에러] ID: {}, Result: {}", event.notificationId(), result);
                notification.markAsFailed();
            }
        } catch (Exception e) {
            log.error(">>>> [슬랙 호출 중 예외 발생] ID: {}, Message: {}", event.notificationId(), e.getMessage());
            notification.markAsFailed();
        }
        notificationRepository.save(notification);
    }
}
