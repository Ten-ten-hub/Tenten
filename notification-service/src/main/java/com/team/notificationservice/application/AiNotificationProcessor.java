package com.team.notificationservice.application;

import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiNotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processAiNotification(AiNotificationRequest aiRequest, String msgType) {
        // 1. 멱등성 체크
        if (notificationRepository.existsByRefId(aiRequest.refId())) {
            return;
        }

        // 2. 엔티티 생성 및 저장
        Notification notification = Notification.builder()
            .receiverId(aiRequest.receiverId())
            .receiverSlackId(aiRequest.receiverSlackId())
            .orderId(aiRequest.orderId())
            .msgType(MsgType.valueOf(msgType))
            .msgContent(aiRequest.msgContent())
            .sendStatus(SendStatus.PENDING)
            .scheduledAt(aiRequest.scheduledAt())
            .refId(aiRequest.refId())
            .build();

        Notification saved = notificationRepository.save(notification);

        // 3. 이벤트 발행
        eventPublisher.publishEvent(new NotificationSavedEvent(
            saved.getId(),
            saved.getReceiverSlackId(),
            saved.getMsgContent()
        ));
    }
}
