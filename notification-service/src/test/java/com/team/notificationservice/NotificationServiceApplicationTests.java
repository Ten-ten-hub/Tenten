package com.team.notificationservice;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team.notificationservice.application.AiNotificationProcessor;
import com.team.notificationservice.application.AiNotificationRequest;
import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSaver;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
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

    @Test
    @DisplayName("성공: Redis에 슬랙 ID 캐시가 있을 경우 외부 API 호출 없이 저장 로직 수행")
    void createAndSend_WithRedisCacheSuccess() {
        // Given
        NotificationRequest req = new NotificationRequest(null, "user@test.com", UUID.randomUUID(), "테스트",
            MsgType.ORDER_ALERT);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("slack:email:user@test.com")).willReturn("U_CACHED_ID");

        // When
        notificationService.createAndSend(req, UUID.randomUUID().toString());

        // Then
        verify(slackClient, never()).findSlackIdByEmail(anyString());
        verify(notificationSaver, times(1)).saveAndPublish(any(), eq("U_CACHED_ID"), any());
    }

    @Test
    @DisplayName("성공: Redis 캐시가 없을 때 Slack API를 통해 ID를 조회하고 캐싱 처리 확인")
    void createAndSend_WithoutCacheApiSuccess() {
        // Given
        NotificationRequest req = new NotificationRequest(null, "new@test.com", null, "메시지", MsgType.ORDER_ALERT);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);
        given(slackClient.findSlackIdByEmail("new@test.com")).willReturn("U_API_ID");

        // When
        notificationService.createAndSend(req, null);

        // Then
        verify(slackClient).findSlackIdByEmail("new@test.com");
        verify(valueOps).set(eq("slack:email:new@test.com"), eq("U_API_ID"), anyLong(), any());
    }

    @Test
    @DisplayName("Kafka: AI 분석 완료 이벤트 수신 시 알림 프로세서 호출 확인")
    void kafkaConsumer_ProcessorExecutionCheck() {
        AiNotificationRequest aiReq = new AiNotificationRequest(UUID.randomUUID(), UUID.randomUUID(), "U1", "결과", null,
            UUID.randomUUID(), "ORDER_ALERT");

        notificationService.consumeAiNotification().accept(aiReq);

        verify(aiNotificationProcessor, times(1)).processAiNotification(eq(aiReq), eq("ORDER_ALERT"));
    }

    @Test
    @DisplayName("성공: 특정 알림 ID로 조회 시 도메인 객체 반환 확인")
    void getNotification_BusinessLogicSuccess() {
        UUID id = UUID.randomUUID();
        com.team.notificationservice.domain.Notification mockNoti =
            com.team.notificationservice.domain.Notification.builder().msgContent("내용").receiverSlackId("U1").build();
        given(notificationRepository.findByIdAndDeletedAtIsNull(id)).willReturn(java.util.Optional.of(mockNoti));

        assertNotNull(notificationService.getNotification(id));
    }
}
