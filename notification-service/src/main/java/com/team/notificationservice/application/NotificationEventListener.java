package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
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
        try {
            notification = notificationRepository.findById(event.notificationId())
                .orElseThrow(() -> new IllegalStateException("알림 엔티티를 찾을 수 없습니다: ID=" + event.notificationId()));

            // 중복 전송 방지: 이미 DB상 성공 상태라면 종료
            if (notification.getSendStatus() == SendStatus.SUCCESS) {
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
                redisTemplate.delete(lockKey); // 성공 시 락 해제 (또는 중복방지 위해 유지 가능)
            } else if (result == SlackSendResult.RETRYABLE_FAILURE) {
                notification.markAsFailed();
                redisTemplate.delete(lockKey); // 명확한 실패 시 재시도를 위해 락 해제
            } else {
                // UNKNOWN(타임아웃 등): 결과가 불분명하므로 락을 유지하여 자동 재시도로 인한 중복 발송 방지
                log.warn("전송 결과 불분명(타임아웃 등) - 중복 방지를 위해 락을 유지합니다: ID={}", event.notificationId());
                notification.markAsFailed(); // 상태는 실패로 기록하되 락은 삭제하지 않음
            }
        } catch (Exception e) {
            log.error("슬랙 전송 처리 중 오류: notificationId={}", event.notificationId(), e);
            notification.markAsFailed();
            redisTemplate.delete(lockKey);
        } finally {
            // 기존의 루프 대신, 새 트랜잭션(REQUIRES_NEW)을 사용하는 별도 서비스에서 저장을 시도함
            // 루프 안에서 saveAndFlush 실패 시 해당 트랜잭션이 Rollback-only가 되는 문제를 해결
            notificationPersistenceService.saveWithRetry(notification);
        }
    }
}
