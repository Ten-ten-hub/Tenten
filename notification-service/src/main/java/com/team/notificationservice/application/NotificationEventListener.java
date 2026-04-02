package com.team.notificationservice.application;

import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SlackClient slackClient;
    private final NotificationRepository notificationRepository;
    // private final KafkaTemplate<String, NotificationCreatedEvent> kafkaTemplate; // TODO: Kafka 도입 시 주입 예정

    // 알림 생성 이벤트를 처리 TransactionPhase.AFTER_COMMIT: 메인 비즈니스 로직이 DB에 완전히 커밋된 후 실행됨
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
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
            // 1. 외부 서비스(슬랙) 호출
            boolean success = slackClient.sendDirectMessage(event.receiverSlackId(), event.message());

            // 2. 알림 엔티티 조회 및 상태 업데이트
            notificationRepository.findById(event.notificationId()).ifPresentOrElse(notification -> {
                if (success) {
                    notification.markAsSuccess();
                    log.info("슬랙 전송 성공: notificationId={}", event.notificationId());
                } else {
                    notification.markAsFailed();
                    log.warn("슬랙 전송 실패(응답 False): notificationId={}", event.notificationId());
                }
                notificationRepository.save(notification); // 변경 사항 명시적 저장
            }, () -> log.error("알림 엔티티를 찾을 수 없습니다: ID={}", event.notificationId()));

        } catch (Exception e) {
            log.error("슬랙 전송 중 예외 발생: {}", e.getMessage());
            // 예외 발생 시에도 실패 상태를 기록
            notificationRepository.findById(event.notificationId()).ifPresent(n -> {
                n.markAsFailed();
                notificationRepository.save(n);
            });
            // AFTER_COMMIT 리스너이므로 사용자 응답에 영향을 주지 않기 위해 예외를 밖으로 던지지 않음
        }
    }
}
