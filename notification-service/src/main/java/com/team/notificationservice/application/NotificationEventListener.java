package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        log.info("이벤트 수신 - 알림 전송 시작: ID={}", event.notificationId());

        // 0. Redis를 이용한 중복 전송 방지
        String lockKey = "noti:lock:" + event.notificationId();
        // 5분간 유효한 락 설정
        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(lockKey, "processing", 5, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.warn("이미 처리 중이거나 전송 완료된 알림입니다: ID={}", event.notificationId());
            return;
        }

        /*
         * [Kafka/RabbitMQ 전환할 때 참고]
         * ---------------------------------------------------------------------------
         * 1. 아래의 slackClient 호출 로직을 제거
         * 2. kafkaTemplate.send("notification-topic", event); 를 호출하여 메시지 브로커로 전달
         * 3. 별도의 'Consumer' 서비스에서 이 메시지를 받아 실제 슬랙 전송을 처리하게 됨
         * ---------------------------------------------------------------------------
         */

        // 1. 엔티티 조회 (AFTER_COMMIT 단계이므로 즉시 조회 가능)
        Notification notification;
        try {
            notification = notificationRepository.findById(event.notificationId())
                .orElseThrow(() -> new IllegalStateException("알림 엔티티를 찾을 수 없습니다: ID=" + event.notificationId()));
        } catch (Exception e) {
            log.error("알림 처리 실패 - 엔티티 조회 불가: notificationId={}", event.notificationId(), e);
            redisTemplate.delete(lockKey); // 실패 시 다시 시도할 수 있도록 락 해제
            return;
        }
        try {
            // 2. 외부 서비스(슬랙) 호출
            boolean success = slackClient.sendDirectMessage(event.receiverSlackId(), event.message());

            if (success) {
                notification.markAsSuccess();
                // 전송 성공 시 락을 유지하여 중복 방지 (성공 상태는 DB가 최종 보장)
            } else {
                notification.markAsFailed();
                redisTemplate.delete(lockKey); // API 호출 자체 실패 시 재시도를 위해 락 해제
            }
        } catch (Exception e) {
            log.error("슬랙 전송 처리 중 오류: notificationId={}", event.notificationId(), e);
            notification.markAsFailed();
            redisTemplate.delete(lockKey);
        } finally {
            try {
                notificationRepository.saveAndFlush(notification);
            } catch (Exception saveEx) {
                log.error("알림 상태 저장 실패: notificationId={}", event.notificationId(), saveEx);
            }
        }
    }
}
