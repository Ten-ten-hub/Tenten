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
    // private final KafkaTemplate<String, NotificationCreatedEvent> kafkaTemplate; // TODO: Kafka 도입 시 주입 예정

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // 알림 생성 이벤트를 처리 TransactionPhase.AFTER_COMMIT: 메인 비즈니스 로직이 DB에 완전히 커밋된 후 실행됨
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        try {
            Thread.sleep(500); // 0.5초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("이벤트 수신 - 알림 전송 시작: ID={}", event.notificationId());

        /*
         * [미래의 Kafka/RabbitMQ 전환 가이드]
         * ---------------------------------------------------------------------------
         * 1. 아래의 slackClient 호출 로직을 제거
         * 2. kafkaTemplate.send("notification-topic", event); 를 호출하여 메시지 브로커로 전달
         * 3. 별도의 'Consumer' 서비스에서 이 메시지를 받아 실제 슬랙 전송을 처리하게 됨
         * ---------------------------------------------------------------------------
         */

        try {
            // 1. 엔티티 조회 (타이밍 이슈 방지를 위해 최대 3번 재시도)
            Notification notification = null;
            for (int i = 0; i < 3; i++) {
                notification = notificationRepository.findById(event.notificationId()).orElse(null);
                if (notification != null) break;
                Thread.sleep(200); // 0.2초 대기 후 재시도
            }

            if (notification == null) {
                log.error("알림 엔티티를 찾을 수 없습니다: ID={}", event.notificationId());
                return;
            }

            // 2. 외부 서비스(슬랙) 호출
            boolean success = slackClient.sendDirectMessage(event.receiverSlackId(), event.message());

            if (success) {
                notification.markAsSuccess();
            } else {
                notification.markAsFailed();
            }
            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("슬랙 전송 처리 중 오류: {}", e.getMessage());
        }
    }
}
