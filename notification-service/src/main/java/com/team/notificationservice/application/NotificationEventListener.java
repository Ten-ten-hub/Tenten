package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 알림 생성 이벤트를 처리 TransactionPhase.AFTER_COMMIT: 메인 비즈니스 로직이 DB에 완전히 커밋된 후 실행됨
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 별도의 트랜잭션에서 전송 결과(성공/실패)를 기록함
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

            // 2. 알림 엔티티 조회 (커밋된 이후이므로 findById로 조회가 가능)
            Notification notification = notificationRepository.findById(event.notificationId())
                .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));

            if (success) {
                notification.markAsSuccess();
                log.info("슬랙 전송 성공: notificationId={}", event.notificationId());
            } else {
                notification.markAsFailed();
                log.warn("슬랙 전송 실패(API 응답 False): notificationId={}", event.notificationId());
            }
        } catch (Exception e) {
            log.error("슬랙 전송 중 예외 발생: {}", e.getMessage());
            // 예외 발생 시에도 전송 실패 상태를 DB에 남기기 위해 다시 조회하여 마킹
            notificationRepository.findById(event.notificationId())
                .ifPresent(Notification::markAsFailed);

            throw new ServiceException(ErrorCode.NOTI_SLACK_API_ERROR);
        }
    }
}
