package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
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

    @Async // 비동기 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새 트랜잭션에서 상태 업데이트
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // DB 저장 성공 후 실행
    public void handleNotificationSavedEvent(NotificationSavedEvent event) {
        log.info(">>>> [이벤트 수신] 슬랙 발송 시작: ID={}", event.notificationId());

        Notification notification = notificationRepository.findById(event.notificationId())
            .orElse(null);

        if (notification == null) {
            return;
        }

        try {
            // 실제 슬랙 발송 호출
            slackClient.sendDirectMessage(event.receiverSlackId(), event.msgContent());

            // 성공 상태 기록
            notification.markAsSentImmediately();
            log.info(">>>> [슬랙 발송 성공] ID={}", event.notificationId());
        } catch (Exception e) {
            log.error(">>>> [슬랙 발송 실패] ID={}, 사유={}", event.notificationId(), e.getMessage());
            notification.markAsFailed();
        }

        notificationRepository.save(notification);
    }
}
