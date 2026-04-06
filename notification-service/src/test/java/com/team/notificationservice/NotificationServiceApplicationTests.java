package com.team.notificationservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team.notificationservice.application.AiNotificationProcessor;
import com.team.notificationservice.application.AiNotificationRequest;
import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSaver;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.infrastructure.SlackClient;
import com.team.notificationservice.presentation.NotificationResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class NotificationServiceApplicationTests {

    @InjectMocks
    private NotificationService notificationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SlackClient slackClient;
    @Mock
    private NotificationSaver notificationSaver;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AiNotificationProcessor aiNotificationProcessor;
    @Mock
    private ValueOperations<String, String> valueOps;

    //1. 정상 작동
    @Test
    @DisplayName("성공: Redis 캐시가 있을 때 슬랙 ID 조회 없이 발송 처리")
    void createAndSend_CacheHit_Success() {
        NotificationRequest req = new NotificationRequest(null, "user@test.com", UUID.randomUUID(), "메시지",
            MsgType.ORDER_ALERT);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        // thenReturn 대신 willReturn 사용
        given(valueOps.get("slack:email:user@test.com")).willReturn("U_CACHED_ID");

        notificationService.createAndSend(req, "USER_ID");

        verify(slackClient, never()).findSlackIdByEmail(anyString());
        verify(notificationSaver).saveAndPublish(any(), eq("U_CACHED_ID"), any());
    }

    @Test
    @DisplayName("성공: Redis 캐시가 없으면 슬랙 API 호출 및 결과 캐싱")
    void createAndSend_CacheMiss_Success() {
        NotificationRequest req = new NotificationRequest(null, "new@test.com", null, "메시지", MsgType.ORDER_ALERT);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);
        given(slackClient.findSlackIdByEmail("new@test.com")).willReturn("U_API_ID");

        notificationService.createAndSend(req, null);

        verify(slackClient).findSlackIdByEmail("new@test.com");
        verify(valueOps).set(eq("slack:email:new@test.com"), eq("U_API_ID"), anyLong(), any());
        // 캐시 미스 시에도 최종적으로 saver가 호출되는지 verify 추가
        verify(notificationSaver, times(1)).saveAndPublish(eq(req), eq("U_API_ID"), any());
    }

    //2. 장애 대응 (Fail-open 테스트)
    @Test
    @DisplayName("장애대응: Redis 조회 장애 발생 시에도 Slack API를 통해 알림 발송")
    void createAndSend_RedisGetFailure_FallbackToApi() {
        NotificationRequest req = new NotificationRequest(null, "fail@test.com", null, "메시지", MsgType.ORDER_ALERT);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        // Redis 조회 시 예외 발생 (willReturn 대신 willThrow 사용)
        given(valueOps.get(anyString())).willThrow(new RuntimeException("Redis Down"));
        given(slackClient.findSlackIdByEmail("fail@test.com")).willReturn("U_API_ID");

        assertDoesNotThrow(() -> notificationService.createAndSend(req, null));
        verify(slackClient).findSlackIdByEmail("fail@test.com");
        verify(notificationSaver).saveAndPublish(any(), eq("U_API_ID"), any());
    }

    @Test
    @DisplayName("장애대응: Slack ID 조회 후 Redis 저장 장애 발생해도 발송 완료")
    void createAndSend_RedisSetFailure_Success() {
        NotificationRequest req = new NotificationRequest(null, "setfail@test.com", null, "메시지", MsgType.ORDER_ALERT);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);
        given(slackClient.findSlackIdByEmail("setfail@test.com")).willReturn("U_API_ID");
        // Redis 저장(set) 시 예외 발생 시나리오
        doThrow(new RuntimeException("Redis Set Error")).when(valueOps).set(anyString(), anyString(), anyLong(), any());

        assertDoesNotThrow(() -> notificationService.createAndSend(req, null));
        verify(notificationSaver).saveAndPublish(any(), eq("U_API_ID"), any());
    }

    //3. Kafka 및 기타
    @Test
    @DisplayName("Kafka: AI 알림 수신 시 프로세서 작동 확인")
    void consumeKafka_Success() {
        AiNotificationRequest aiReq = new AiNotificationRequest(UUID.randomUUID(), UUID.randomUUID(), "U1", "결과", null,
            UUID.randomUUID(), "ORDER_ALERT");
        notificationService.consumeAiNotification().accept(aiReq);
        verify(aiNotificationProcessor, times(1)).processAiNotification(eq(aiReq), eq("ORDER_ALERT"));
    }

    @Test
    @DisplayName("엣지케이스: AI 프로세서에서 예외 발생 시 전파 확인")
    void kafkaConsumer_ProcessorExceptionPath() {
        AiNotificationRequest aiReq = new AiNotificationRequest(UUID.randomUUID(), UUID.randomUUID(), "U1", "결과", null,
            UUID.randomUUID(), "ORDER_ALERT");
        doThrow(new RuntimeException("Processing Failed")).when(aiNotificationProcessor)
            .processAiNotification(any(), anyString());

        assertThrows(RuntimeException.class, () -> notificationService.consumeAiNotification().accept(aiReq));
    }

    @Test
    @DisplayName("성공: 알림 단건 조회 로직 및 반환 필드 값 검증")
    void getNotification_Success() {
        // Given
        UUID id = UUID.randomUUID();
        Notification mockNoti = Notification.builder()
            .msgContent("상세내용 확인")
            .receiverSlackId("U_FIXED_123")
            .sendStatus(SendStatus.SUCCESS)
            .build();
        given(notificationRepository.findByIdAndDeletedAtIsNull(id)).willReturn(java.util.Optional.of(mockNoti));

        // When
        NotificationResponse response = notificationService.getNotification(id);

        // Then - 상세 필드 검증 추가
        assertNotNull(response);
        assertEquals("상세내용 확인", response.message());
        assertEquals(SendStatus.SUCCESS, response.status());
        verify(notificationRepository).findByIdAndDeletedAtIsNull(id);
    }
}
