package com.team.notificationservice.application;

import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
        // 1. 1차 체크
        if (notificationRepository.existsByRefId(aiRequest.refId())) {
            log.info(">>>> [중복 메시지] 이미 처리됨(exists): {}", aiRequest.refId());
            return;
        }
        try {

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

            eventPublisher.publishEvent(new NotificationSavedEvent(
                saved.getId(), saved.getReceiverSlackId(), saved.getMsgContent()
            ));
        } catch (
            DataIntegrityViolationException e) {
            // 동시성 이슈로 인한 DB 제약조건 위반 처리
            log.warn(">>>> [중복 메시지 방어] 거의 동시에 들어온 동일 RefID 차단: {}", aiRequest.refId());
            // 예외를 던지지 않고 정상 종료하여 Kafka 메시지를 성공 처리(ACK)함
        }
    }
}
