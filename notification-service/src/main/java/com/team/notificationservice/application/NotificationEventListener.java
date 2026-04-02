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

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        log.info("이벤트 수신 - 알림 전송 시작: ID={}", event.notificationId());

        /*
         * [Kafka/RabbitMQ 전환할 때 참고]
         * ---------------------------------------------------------------------------
         * 1. 아래의 slackClient 호출 로직을 제거
         * 2. kafkaTemplate.send("notification-topic", event); 를 호출하여 메시지 브로커로 전달
         * 3. 별도의 'Consumer' 서비스에서 이 메시지를 받아 실제 슬랙 전송을 처리하게 됨
         * ---------------------------------------------------------------------------
         */

        // 1. 엔티티 조회 (AFTER_COMMIT 단계이므로 즉시 조회 가능)
        Notification notification = notificationRepository.findById(event.notificationId())
            .orElseThrow(() -> new IllegalStateException("알림 엔티티를 찾을 수 없습니다: ID=" + event.notificationId()));

        try {
            // 2. 외부 서비스(슬랙) 호출
            boolean success = slackClient.sendDirectMessage(event.receiverSlackId(), event.message());

            if (success) {
                notification.markAsSuccess();
            } else {
                notification.markAsFailed();
            }
        } catch (Exception e) {
            log.error("슬랙 전송 처리 중 오류: {}", e.getMessage());
            notification.markAsFailed();
        } finally {
            notificationRepository.save(notification);
        }
    }
}
