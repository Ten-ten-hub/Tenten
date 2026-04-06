package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.infrastructure.SlackSendResult;
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
    private final NotificationPersistenceService notificationPersistenceService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        log.info("이벤트 수신 - 알림 전송 시작: ID={}", event.notificationId());

        // 0. Redis를 이용한 중복 전송 방지
        String lockKey = "noti:lock:" + event.notificationId();
        Boolean isFirstRequest = true; // 기본값 true (Fail-open용)

        try {
            // 5분간 유효한 락 설정
            isFirstRequest = redisTemplate.opsForValue().setIfAbsent(lockKey, "processing", 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis 연결 실패 시 중단하지 않고 전송을 계속함 (Fail-open)
            log.warn("Redis 연결 실패 - Fail-open 전략에 따라 전송을 계속합니다: {}", e.getMessage());
        }

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
        boolean shouldReleaseLock = false;
        try {
            notification = notificationRepository.findById(event.notificationId())
                .orElseThrow(() -> new IllegalStateException("알림 엔티티를 찾을 수 없습니다: ID=" + event.notificationId()));

            // 중복 전송 방지: 이미 DB상 성공 상태라면 종료
            if (notification.getSendStatus().isDelivered()) {
                log.info("이미 성공 처리된 알림입니다. 전송을 중단합니다: ID={}", event.notificationId());
                return;
            }
        } catch (Exception e) {
            log.error("알림 처리 실패 - 엔티티 조회 불가: notificationId={}", event.notificationId(), e);
            redisTemplate.delete(lockKey);
            return;
        }

        try {
            // 2. 외부 서비스(슬랙) 호출
            // 단순 boolean 대신 세분화된 결과(SUCCESS, RETRYABLE_FAILURE, UNKNOWN)를 받음
            SlackSendResult result = slackClient.sendDirectMessage(event.receiverSlackId(), event.message());

            if (result == SlackSendResult.SUCCESS) {
                notification.markAsSuccess();
                shouldReleaseLock = true;
            } else if (result == SlackSendResult.RETRYABLE_FAILURE) {
                notification.markAsFailed();
                shouldReleaseLock = true;
            } else {
                // TODO: 브로커 도입 시 UNKNOWN 메시지를 큐에 남겨두거나 별도 검수 큐로 보낼 수 있음
                log.warn("전송 결과 불분명 - 상태를 유지하고 락을 보존: ID={}", event.notificationId());
                shouldReleaseLock = false;
            }

            // 상태 저장 실행
            notificationPersistenceService.saveWithRetry(notification);

            // DB 저장이 성공한 경우에만 락 해제
            if (shouldReleaseLock) {
                safeDeleteLock(lockKey);
            }

        } catch (Exception e) {
            log.error("이벤트 처리 중 예외 발생: ID={}", event.notificationId(), e);
            // TODO: 브로커 도입 시 예외 발생 시 메시지 승인(Ack)을 하지 않음으로써 자동 재시도를 유도함
            safeDeleteLock(lockKey);
        }
    }

    // Redis 삭제 실패가 비즈니스 로직에 영향을 주지 않도록 하는 헬퍼 메서드
    private void safeDeleteLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // TODO: 브로커 도입 시 수동 Redis 락 관리 로직이 제거되거나 대폭 단순화될 예정
            log.warn("Redis 락 해제 실패 (Fail-open): {}", e.getMessage());
        }
    }
}
