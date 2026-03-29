package com.team.notificationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSearchCondition;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.presentation.NotificationResponse;
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

@SpringBootTest
class NotificationServiceApplicationTests {

    @Autowired
    private NotificationService notificationService;
    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean// 실제 슬랙 API 서버를 호출하지 않도록 가짜 객체(Mock) 등록
    private SlackClient slackClient;

    @Test
    @DisplayName("슬랙 알림 생성 및 발송 프로세스 통합 테스트")
    void notificationSendTest() {
        // given: 테스트용 데이터 준비 (현재 DTO 구조 반영)
        NotificationRequest request = new NotificationRequest();
        request.setReceiverSlackId("U12345678");
        request.setOrderId(UUID.randomUUID()); // UUID 타입 반영
        request.setMessage("테스트 알림 메시지입니다.");
        request.setMsgType(MsgType.ORDER_ALERT);

        // 슬랙 발송 메서드가 호출되면 무조건 true를 반환하도록 설정
        when(slackClient.sendDirectMessage(anyString(), anyString())).thenReturn(true);

        // when: 알림 서비스 호출
        notificationService.createAndSend(request);

        // then: slackClient의 sendDirectMessage 메서드가 실제로 호출되었는지 검증
        verify(slackClient, times(1)).sendDirectMessage(eq("U12345678"), eq("테스트 알림 메시지입니다."));
    }

    @Test
    @DisplayName("이메일 정보만 제공될 경우 슬랙 ID를 조회하여 메시지를 발송하는지 검증")
    void notificationSendWithEmailTest() {
        // given
        NotificationRequest request = new NotificationRequest();
        request.setEmail("test@example.com");
        request.setOrderId(UUID.randomUUID());
        request.setMessage("이메일 기반 ID  조회 테스트");
        request.setMsgType(MsgType.ORDER_ALERT);

        // 가짜 동작 정의: 이메일로 조회 시 특정 슬랙 ID 반환
        when(slackClient.findSlackIdByEmail("test@example.com")).thenReturn("U_SEARCHED_ID");
        when(slackClient.sendDirectMessage(anyString(), anyString())).thenReturn(true);

        // when
        notificationService.createAndSend(request);

        // then: 1. 이메일 조회가 발생했는지 확인, 2. 조회된 ID로 발송되었는지 확인
        verify(slackClient, times(1)).findSlackIdByEmail("test@example.com");
        verify(slackClient, times(1)).sendDirectMessage(eq("U_SEARCHED_ID"), anyString());
    }

    @Test
    @DisplayName("슬랙 ID와 키워드로 검색 시 검색 결과가 반환되는지 확인")
    void searchNotificationsMockTest() {
        // given
        String slackId = "U12345678";
        String keyword = "배송";

        // 1. 가짜 결과 데이터 생성
        Notification mockNotification = Notification.builder()
                .msgContent("배송이 시작되었습니다.")
                .receiverSlackId(slackId)
                .sendStatus(SendStatus.SUCCESS)
                .build();
        Page<Notification> mockPage = new PageImpl<>(List.of(mockNotification));

        // 2. 레포지토리가 이 데이터를 주도록 Mock 설정
        when(notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(
                anyString(), anyString(), any(Pageable.class)))
                .thenReturn(mockPage);

        // when
        NotificationSearchCondition condition = new NotificationSearchCondition();
        condition.setSlackId(slackId);
        condition.setKeyword(keyword);
        Page<NotificationResponse> result = notificationService.searchNotifications(condition, PageRequest.of(0, 10));

        // then
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getMessage().contains(keyword));
    }

    @Test
    @DisplayName("알림 ID로 단건 조회를 수행한다")
    void getNotificationTest() {
        // given
        UUID notificationId = UUID.randomUUID();
        Notification mockNotification = Notification.builder()
                .msgContent("단건 조회 테스트 메시지")
                .receiverSlackId("U12345678")
                .sendStatus(SendStatus.SUCCESS)
                .build();

        // 레포지토리 Mock 설정
        when(notificationRepository.findByIdAndDeletedAtIsNull(notificationId))
                .thenReturn(Optional.of(mockNotification));

        // when
        NotificationResponse response = notificationService.getNotification(notificationId);

        // then
        assertNotNull(response);
        assertEquals("단건 조회 테스트 메시지", response.getMessage());
        assertEquals(SendStatus.SUCCESS, response.getStatus());
    }
}