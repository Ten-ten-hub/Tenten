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
     *
     * @param dto           알림 요청 정보
     * @param targetSlackId 식별된 슬랙 ID
     * @param receiverId    식별된 수신자 UUID (없을 경우 null 전달)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAndPublish(NotificationRequest dto, String targetSlackId, UUID receiverId) {

        //TODO: 수신자 ID 연동 완료 시 아래 블록을 삭제하고 에러 처리로 변경할 것
        if (receiverId == null) {
            receiverId = Constants.SYSTEM_UUID;
            // 나중에 엄격하게 하려면 여기서 throw new ServiceException(...)을 던지도록 수정해야함
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

        // 슬랙 발송을 위한 이벤트 발행
        eventPublisher.publishEvent(new NotificationCreatedEvent(
            notification.getId(),
            targetSlackId,
            notification.getMsgContent()
        ));
    }
}
