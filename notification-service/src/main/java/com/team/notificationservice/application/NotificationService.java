package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.infrastructure.SlackClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackClient slackClient;

    @Transactional
    public void createAndSend(NotificationRequest dto) {
        String targetSlackId = dto.getReceiverSlackId();

        // ID가 없고 이메일이 있다면 이메일로 ID 조회
        if ((targetSlackId == null || targetSlackId.isEmpty()) && dto.getEmail() != null) {
            targetSlackId = slackClient.findSlackIdByEmail(dto.getEmail());
        }

        if (targetSlackId == null) {
            log.error("대상자를 특정할 수 없습니다.");
            return;
        }

        Notification notification = Notification.builder()
                .receiverSlackId(targetSlackId) // 조회된 혹은 입력된 ID 저장
                .orderId(dto.getOrderId())
                .msgContent(dto.getMessage())
                .msgType(dto.getMsgType())
                .sendStatus(SendStatus.PENDING)
                .build();

        notificationRepository.save(notification);

        // 실제 발송
        boolean success = slackClient.sendDirectMessage(targetSlackId, notification.getMsgContent());

        if (success) {
            notification.markAsSuccess();
        } else {
            notification.markAsFailed();
        }
    }
}