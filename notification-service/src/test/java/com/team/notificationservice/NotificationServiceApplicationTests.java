package com.team.notificationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team.common.page.PageResponse;
import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSaver;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest(properties = {
    "spring.config.import=optional:file:../application-common.properties,optional:file:../application-secret.properties"
})
@RecordApplicationEvents
class NotificationServiceApplicationTests {

    @Autowired
    private NotificationService notificationService;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private SlackClient slackClient;

    @MockitoBean
    private NotificationSaver notificationSaver;

    @MockitoBean // Redis 템플릿 모킹
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ApplicationEvents applicationEvents;

    @org.junit.jupiter.api.BeforeEach
    void setupRedisMock() {
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps = mock(
            org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("슬랙 알림 생성 및 이벤트 발행 검증")
    void notificationSendTest() {
        // given
        NotificationRequest request = new NotificationRequest("U12345678", null, UUID.randomUUID(), "테스트 메시지",
            MsgType.ORDER_ALERT);

        // resolveTargetSlackId 로직은 내부에서 처리됨
        // when
        notificationService.createAndSend(request, null);

        // then: Saver가 적절한 인자로 호출되었는지 검증
        verify(notificationSaver, times(1)).saveAndPublish(eq(request), anyString(), any());
    }

    @Test
    @DisplayName("이메일 정보만 제공될 경우 슬랙 ID를 조회하여 발송하는지 검증")
    void notificationSendWithEmailTest() {
        // given
        NotificationRequest request = new NotificationRequest(
            null, "test@example.com", UUID.randomUUID(), "이메일 기반 조회 테스트", MsgType.ORDER_ALERT
        );

        // 이메일로 조회 시 가짜 ID 반환 설정
        when(slackClient.findSlackIdByEmail("test@example.com")).thenReturn("U_SEARCHED_ID");

        // when
        notificationService.createAndSend(request, null);

        // then: 1. 이메일 조회가 발생했는가? 2. Saver로 올바른 SlackId가 전달로 발행되었는가?
        verify(slackClient, times(1)).findSlackIdByEmail("test@example.com");

        verify(notificationSaver, times(1)).saveAndPublish(eq(request), eq("U_SEARCHED_ID"), any());
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
    @DisplayName("슬랙 ID와 키워드로 검색 확인 - 결과가 없을 때 빈 페이지 반환")
    void searchNotifications_Empty_Success() {
        // given
        String slackId = "U123";
        // 빈 결과를 반환하도록 설정 (Service에서 더 이상 예외를 던지지 않음)
        when(notificationRepository.findByReceiverSlackIdAndMsgContentContainingAndDeletedAtIsNull(anyString(),
            anyString(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        // when
        NotificationSearchCondition cond = new NotificationSearchCondition(slackId, "배송");
        Page<NotificationResponse> result = notificationService.searchNotifications(cond, PageRequest.of(0, 10));

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
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
    @DisplayName("삭제 테스트 - 실제 유저 ID가 있을 때 그대로 저장되는지 확인")
    void deleteNotification_WithActualUser() {
        // given
        UUID id = UUID.randomUUID();
        UUID actualAdminId = UUID.randomUUID(); // 실제 유저 UUID
        Notification mockNoti = Notification.builder().msgContent("삭제").build();
        when(notificationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(mockNoti));

        // when
        notificationService.deleteNotification(id, actualAdminId.toString());

        // then: 전달한 actualAdminId가 그대로 박혀야 함
        assertEquals(actualAdminId, mockNoti.getDeletedBy());
    }

    @Test
    @DisplayName("삭제 테스트 - SYSTEM일 때 시스템 기본 ID로 저장되는지 확인")
    void deleteNotification_WithSystem() {
        // given
        UUID id = UUID.randomUUID();
        Notification mockNoti = Notification.builder().msgContent("삭제").build();
        when(notificationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(mockNoti));

        // when
        notificationService.deleteNotification(id, "SYSTEM");

        // then: 0000... 시스템 ID 확인
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), mockNoti.getDeletedBy());
    }

    @Test
    @DisplayName("common 규격 PageResponse 변환 검증")
    void checkPageResponseMapping() {
        // given
        Notification mockNoti = Notification.builder().msgContent("테스트").receiverSlackId("U1").build();
        Page<NotificationResponse> resultPage = new PageImpl<>(List.of(NotificationResponse.from(mockNoti)),
            PageRequest.of(0, 10), 1);

        // when
        PageResponse<NotificationResponse> response = PageResponse.from(resultPage);

        // then
        assertEquals(1, response.content().size());
        assertEquals(1, response.pageInfo().currentPage());
    }

    @Test
    @DisplayName("컨트롤러의 1-based 페이지가 서비스 요청 시 0-based Pageable로 처리되는지 검증")
    void verifyPagingConversion() {
        // given
        String slackId = "U123";
        NotificationSearchCondition cond = new NotificationSearchCondition(slackId, null);

        // 컨트롤러에서 page=1 요청 시 생성될 0-based Pageable (pageNumber = 0)
        Pageable zeroBasedPageable = PageRequest.of(0, 10);

        when(notificationRepository.findByReceiverSlackIdAndDeletedAtIsNull(eq(slackId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        // when
        notificationService.searchNotifications(cond, zeroBasedPageable);

        // then: 리포지토리에 전달된 Pageable의 페이지 번호가 0인지 확인
        verify(notificationRepository).findByReceiverSlackIdAndDeletedAtIsNull(
            eq(slackId),
            argThat(p -> p.getPageNumber() == 0)
        );
    }

    @Test
    @DisplayName("Redis에 슬랙 ID가 캐싱되어 있다면 외부 API를 호출하지 않는다")
    void notificationSendWithRedisCacheTest() {
        // given
        String email = "cached@example.com";
        String cachedId = "U_CACHED_123";
        NotificationRequest request = new NotificationRequest(
            null, email, UUID.randomUUID(), "캐시 테스트", MsgType.ORDER_ALERT
        );

        // ValueOperations 모킹
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps = mock(
            org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Redis에 이미 값이 있는 상황 설정
        when(valueOps.get("slack:email:" + email)).thenReturn(cachedId);

        // when
        notificationService.createAndSend(request, null);

        // then: 1. Redis에서 값을 조회했는가? 2. SlackClient(외부API)는 호출되지 않았는가?
        verify(valueOps).get("slack:email:" + email);
        verify(slackClient, never()).findSlackIdByEmail(anyString()); // 호출되지 않아야 함
        verify(notificationSaver, times(1)).saveAndPublish(eq(request), eq(cachedId), any());
    }
}
