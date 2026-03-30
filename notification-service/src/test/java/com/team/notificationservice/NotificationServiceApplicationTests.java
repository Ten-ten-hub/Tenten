package com.team.notificationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team.notificationservice.application.NotificationCreatedEvent;
import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSearchCondition;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.presentation.NotificationResponse;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest
@RecordApplicationEvents
class NotificationServiceApplicationTests {

    @Autowired
    private NotificationService notificationService;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private SlackClient slackClient;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    @DisplayName("슬랙 알림 생성 및 이벤트 발행 검증")
    void notificationSendTest() {
        // given
        NotificationRequest request = new NotificationRequest("U12345678", null, UUID.randomUUID(), "테스트 메시지",
            MsgType.ORDER_ALERT);
        Notification savedNoti = Notification.builder().msgContent("테스트").build();
        when(notificationRepository.save(any())).thenReturn(savedNoti);

        // when
        notificationService.createAndSend(request);

        // then
        // 이벤트 발행 확인
        long count = applicationEvents.stream(NotificationCreatedEvent.class).count();
        assertEquals(1, count);
    }

    @Test
    @DisplayName("이메일 정보만 제공될 경우 슬랙 ID를 조회하여 발송하는지 검증")
    void notificationSendWithEmailTest() {
        // given
        NotificationRequest request = new NotificationRequest(
            null, "test@example.com", UUID.randomUUID(), "이메일 기반 조회 테스트", MsgType.ORDER_ALERT
        );

        Notification savedNoti = Notification.builder().msgContent("이메일 테스트").build();
        when(notificationRepository.save(any())).thenReturn(savedNoti);

        // 이메일로 조회 시 가짜 ID 반환 설정
        when(slackClient.findSlackIdByEmail("test@example.com")).thenReturn("U_SEARCHED_ID");

        // when
        notificationService.createAndSend(request);

        // then: 1. 이메일 조회가 발생했는가? 2. 이벤트가 조회된 ID로 발행되었는가?
        verify(slackClient, times(1)).findSlackIdByEmail("test@example.com");

        long count = applicationEvents.stream(NotificationCreatedEvent.class)
            .filter(event -> event.receiverSlackId().equals("U_SEARCHED_ID"))
            .count();
        assertEquals(1, count);
    }

    @Test
    @DisplayName("슬랙 ID와 키워드로 검색 확인 - 결과가 있을 때")
    void searchNotificationsMockTest() {
        // given
        String slackId = "U123";
        Notification mockNoti = Notification.builder().msgContent("배송 완료").receiverSlackId(slackId).build();
        // condition.keyword()가 존재할 때 호출되는 메서드를 stubbing
        when(notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(
            eq(slackId), eq("배송"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(mockNoti)));

        // when
        NotificationSearchCondition cond = new NotificationSearchCondition(slackId, "배송");
        Page<NotificationResponse> result = notificationService.searchNotifications(cond, PageRequest.of(0, 10));

        // then
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("키워드 없이 슬랙 ID로만 검색 확인 - 결과가 있을 때")
    void searchNotifications_OnlySlackId_Success() {
        // given
        String slackId = "U123";
        Notification mockNoti = Notification.builder().msgContent("전체 메시지").receiverSlackId(slackId).build();

        // keyword가 null일 때 호출되는 메서드를 stubbing
        when(notificationRepository.findByReceiverSlackIdAndDeletedAtIsNull(eq(slackId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(mockNoti)));

        // when
        NotificationSearchCondition cond = new NotificationSearchCondition(slackId, null);
        Page<NotificationResponse> result = notificationService.searchNotifications(cond, PageRequest.of(0, 10));

        // then
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("슬랙 ID와 키워드로 검색 확인 - 결과가 없을 때 예외 발생")
    void searchNotifications_Empty_Fail() {
        // given
        String slackId = "U123";
        // 빈 결과를 반환하도록 설정
        when(notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(anyString(),
            anyString(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        // when & then
        NotificationSearchCondition cond = new NotificationSearchCondition(slackId, "배송");
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            notificationService.searchNotifications(cond, PageRequest.of(0, 10));
        });

        assertEquals(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("단건 조회 테스트")
    void getNotificationTest() {
        UUID id = UUID.randomUUID();
        Notification mockNoti = Notification.builder().msgContent("테스트").receiverSlackId("U1")
            .sendStatus(SendStatus.SUCCESS).build();
        when(notificationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(mockNoti));

        NotificationResponse response = notificationService.getNotification(id);

        assertNotNull(response);
        assertEquals("테스트", response.message());
    }

    @Test
    @DisplayName("삭제(Soft Delete) 테스트")
    void deleteNotificationTest() {
        UUID id = UUID.randomUUID();
        Notification mockNoti = Notification.builder().msgContent("삭제").build();
        when(notificationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(mockNoti));

        notificationService.deleteNotification(id, "user-123");

        assertNotNull(mockNoti.getDeletedAt());
        assertEquals("user-123", mockNoti.getDeletedBy());
    }
}
