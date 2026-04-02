package com.team.notificationservice.application;

import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.presentation.NotificationResponse;
import com.team.notificationservice.presentation.common.Constants;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackClient slackClient;// Listener로 옮길 예정이지만, ID 조회 로직 때문에 유지
    private final NotificationSaver notificationSaver;

    /**
     * 알림 생성 및 전송 엔트리 포인트 네트워크 호출(Slack API)을 포함하므로 @Transactional을 붙이지 않음!
     */
    public void createAndSend(NotificationRequest dto, String userIdFromHeader) {
        // 1. 수신자 슬랙 ID 식별
        String targetSlackId = resolveTargetSlackId(dto);

        // 2. 수신자 UUID 결정
        UUID receiverUuid = null;

          //TODO 헤더 작업 완료 시 주석 해제
//        if (userIdFromHeader != null && !userIdFromHeader.isBlank()) {
//            try {
//                receiverUuid = UUID.fromString(userIdFromHeader);
//            } catch (IllegalArgumentException e) {
//                log.warn("Invalid UUID format in header: {}", userIdFromHeader);
//            }
//        }

        if (receiverUuid == null) {
            // 필요 시 테스트용 UUID를 직접 넣거나, null로 보내어 Saver에서 0000... 시스템 ID가 박히게 함
            log.info("헤더값이 없어서 일단 시스템id로 진행 ");
        }

        // 3. 별도 트랜잭션 컴포넌트(Saver)를 통해 저장 및 이벤트 발행
        notificationSaver.saveAndPublish(dto, targetSlackId, receiverUuid);
    }

    /**
     * 수신자 Slack ID 식별 로직
     */
    private String resolveTargetSlackId(NotificationRequest dto) {
        String targetSlackId = dto.receiverSlackId();

        // targetSlackId가 없거나 공백인 경우, 이메일이 유효하다면 Slack API 호출
        if ((targetSlackId == null || targetSlackId.isBlank()) && dto.email() != null && !dto.email().isBlank()) {
            targetSlackId = slackClient.findSlackIdByEmail(dto.email());
        }

        // 최종 결과가 여전히 비어있거나 공백이면 예외 발생
        if (targetSlackId == null || targetSlackId.isBlank()) {
            throw new ServiceException(ErrorCode.NOTI_RECIPIENT_NOT_FOUND);
        }
        return targetSlackId;
    }

    public Page<NotificationResponse> searchNotifications(NotificationSearchCondition condition, Pageable pageable) {
        Page<Notification> result;

        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            result = notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(
                condition.slackId(), condition.keyword(), pageable);
        } else {
            result = notificationRepository.findByReceiverSlackIdAndDeletedAtIsNull(
                condition.slackId(), pageable);
        }

        // 결과가 비어있어도 404를 던지지 않고 빈 페이지 반환 (200 OK 일관성 유지)
        return result.map(NotificationResponse::from);
    }

    public NotificationResponse getNotification(UUID id) {
        return notificationRepository.findByIdAndDeletedAtIsNull(id)
            .map(NotificationResponse::from)
            .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));
    }

    @Transactional
    public void deleteNotification(UUID id, String deletedBy) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));

        // deletedBy가 null/blank/SYSTEM인 경우 조건문으로 처리하고, 그 외의 경우 try-catch를 통해 UUID 파싱 실패 시 시스템 ID로 대체
        UUID adminUuid;
        if (deletedBy == null || deletedBy.isBlank() || Constants.SYSTEM_USER_ID.equals(deletedBy)) {
            adminUuid = Constants.SYSTEM_UUID;
        } else {
            try {
                adminUuid = UUID.fromString(deletedBy);
            } catch (IllegalArgumentException e) {
                adminUuid = Constants.SYSTEM_UUID;
            }
        }

        notification.delete(adminUuid);
        notificationRepository.save(notification);
    }
}
