package com.team.notificationservice.application;

import com.team.common.Constants;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationSaver {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 실제 DB 저장 및 이벤트를 발행하는 로직
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAndPublish(NotificationRequest dto,
                               String targetSlackId,
                               UUID receiverId) {

        if (receiverId == null) {
            receiverId = Constants.SYSTEM_UUID;
        }

        Notification notification = Notification.builder()
            .receiverId(receiverId)
            .receiverSlackId(targetSlackId)
            .orderId(dto.orderId())
            .msgContent(dto.message())
            .msgType(dto.msgType())
            .sendStatus(SendStatus.PENDING)
            .build();

        notificationRepository.saveAndFlush(notification);

        eventPublisher.publishEvent(new NotificationCreatedEvent(
            notification.getId(),
            targetSlackId,
            notification.getMsgContent()
        ));
    }

    /**
     * AI 분석 ID(refId)를 포함하여 저장하고 이벤트를 발행하는 로직
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveWithAiRef(NotificationRequest dto, String targetSlackId, UUID receiverId, UUID aiAnalysisId) {

        if (receiverId == null) {
            receiverId = Constants.SYSTEM_UUID;
        }

        Notification notification = Notification.builder()
            .receiverId(receiverId)
            .receiverSlackId(targetSlackId)
            .orderId(dto.orderId())
            .msgContent(dto.message())
            .msgType(dto.msgType())
            .sendStatus(SendStatus.PENDING)
            .refId(aiAnalysisId) // AI 서비스의 분석 ID 저장
            .build();

        notificationRepository.saveAndFlush(notification);

        // 슬랙 발송 이벤트 발행 (동일한 리스너가 처리)
        eventPublisher.publishEvent(new NotificationCreatedEvent(
            notification.getId(),
            targetSlackId,
            notification.getMsgContent()
        ));
    }
}
