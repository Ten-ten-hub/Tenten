package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    /**
     * 별도의 트랜잭션(REQUIRES_NEW)에서 저장을 실행하여, 이전 트랜잭션의 실패 여부와 상관없이 재시도가 가능하게 합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveWithRetry(Notification notification) {
        int maxAttempts = 3;
        Exception lastException = null;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                saveOnce(notification); // 개별 트랜잭션에서 실행
                return; // 저장 성공 시 즉시 종료
            } catch (Exception e) {
                lastException = e;
                log.error("알림 상태 저장 실패 (시도 {}/{}): ID={}", i + 1, maxAttempts, notification.getId(), e);
                if (i < maxAttempts - 1) {
                    performBackoff(i); // 지수 백오프 적용
                }
            }
        }
        throw new RuntimeException("최종 저장 실패: ID=" + notification.getId(), lastException);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 각 시도를 새 트랜잭션으로 격리
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
