package com.team.notificationservice.application;

import com.team.notificationservice.domain.MsgType;
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
public class AiNotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final SlackClient slackClient;

    @Transactional
    public void processAiNotification(AiNotificationRequest aiRequest, String msgType) {
        // 1. 멱등성 체크: 동일한 refId(AiAnalysis ID)로 이미 처리된 내역이 있는지 확인
        if (notificationRepository.existsByRefId(aiRequest.refId())) {
            log.info(">>>> [중복 메시지 스킵] 이미 처리된 RefID 입니다: {}", aiRequest.refId());
            return; // 이미 저장/발송되었으므로 로직 종료
        }
        
        log.info(">>>> [DB 저장 및 슬랙 발송 시작] RefID: {}", aiRequest.refId());

        MsgType type;
        try {
            type = MsgType.valueOf(msgType);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("[INVALID MSG_TYPE] '{}'. OrderID: {}", msgType, aiRequest.orderId());
            type = MsgType.ORDER_ALERT;
        }

        Notification notification = Notification.builder()
            .receiverId(aiRequest.receiverId())
            .receiverSlackId(aiRequest.receiverSlackId())
            .orderId(aiRequest.orderId())
            .msgType(type)
            .msgContent(aiRequest.msgContent())
            .sendStatus(SendStatus.PENDING)
            .scheduledAt(aiRequest.scheduledAt())
            .refId(aiRequest.refId())
            .build();

        Notification saved = notificationRepository.save(notification);

        try {
            slackClient.sendDirectMessage(saved.getReceiverSlackId(), saved.getMsgContent());
            saved.markAsSentImmediately();
            log.info(">>>> [슬랙 발송 최종 성공] ID: {}, SlackID: {}", saved.getId(), saved.getReceiverSlackId());
        } catch (Exception e) {
            log.error(">>>> [슬랙 API 호출 에러] : {}", e.getMessage());
            saved.markAsFailed();
        }
        notificationRepository.save(saved);
    }
}
