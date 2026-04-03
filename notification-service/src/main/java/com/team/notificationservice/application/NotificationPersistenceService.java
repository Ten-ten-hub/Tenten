package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Lazy // 순환 참조 방지를 위해 지연 주입
    @Autowired
    private NotificationPersistenceService self;

    /**
     * TODO: 메시지 브로커 도입 시 이 메서드 전체가 제거될 수 있음
     * 브로커가 제공하는 재시도(Retry) 및 DLQ 메커니즘이 이 루프를 대체함
     */
    public void saveWithRetry(Notification notification) {
        int maxAttempts = 3;
        Exception lastException = null;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                // 반드시 self 프록시를 통해 호출해야 REQUIRES_NEW가 적용됨
                self.saveOnce(notification);
                return;
            } catch (Exception e) {
                lastException = e;
                log.error("알림 상태 저장 실패 (시도 {}/{}): ID={}", i + 1, maxAttempts, notification.getId(), e);

                if (i < maxAttempts - 1) {
                    performBackoff(i);
                }
            }
        }
        throw new RuntimeException("최종 저장 실패: ID=" + notification.getId(), lastException);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 각 시도를 새 트랜잭션으로 격리
    public void saveOnce(Notification notification) {
        notificationRepository.saveAndFlush(notification);
    }

    private void performBackoff(int attempt) {
        try {
            // 지수 백오프: 100ms * 2^attempt (최대 1초 제한)
            long delay = Math.min(1000L, (long) (100 * Math.pow(2, attempt)));
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
