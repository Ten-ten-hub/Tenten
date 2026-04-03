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
        for (int i = 0; i < maxAttempts; i++) {
            try {
                notificationRepository.saveAndFlush(notification);
                return; // 저장 성공 시 즉시 종료
            } catch (Exception e) {
                log.error("알림 상태 저장 실패 (시도 {}/{}): ID={}", i + 1, maxAttempts, notification.getId(), e);
                if (i == maxAttempts - 1) {
                    log.error("최종 저장 실패 - 수동 조치 필요: ID={}", notification.getId());
                }
                try {
                    Thread.sleep(100 * (i + 1)); // 지수 백오프
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
