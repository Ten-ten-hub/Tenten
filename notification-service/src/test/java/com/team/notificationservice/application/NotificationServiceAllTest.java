package com.team.notificationservice.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.Notification;
import com.team.notificationservice.domain.NotificationRepository;
import com.team.notificationservice.infrastructure.SlackClient;
import java.time.LocalDateTime;
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
public class NotificationServiceAllTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SlackClient slackClient;
    @Mock
    private NotificationSaver notificationSaver;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private AiNotificationProcessor aiNotificationProcessor;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Kafka 컨슈머 로직 검증: AI 분석 결과 수신 시 프로세서 호출")
    void kafkaConsumerTest() {
        // Given
        AiNotificationRequest kafkaRequest = new AiNotificationRequest(
            UUID.randomUUID(), UUID.randomUUID(), "U123", "본문", LocalDateTime.now(), UUID.randomUUID(), "ORDER_ALERT"
        );

        // When
        notificationService.consumeAiNotification().accept(kafkaRequest);

        // Then
        verify(aiNotificationProcessor, times(1)).processAiNotification(eq(kafkaRequest), eq("ORDER_ALERT"));
    }

    @Test
    @DisplayName("수신자 식별 로직 검증: Redis에 없을 때 Slack API를 거쳐 ID 확정")
    void resolveSlackIdTest() {
        // Given
        NotificationRequest dto = new NotificationRequest(null, "user@team.com", null, "메시지", MsgType.ORDER_ALERT);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(slackClient.findSlackIdByEmail("user@team.com")).willReturn("U_NEW_ID");

        // When
        notificationService.createAndSend(dto, UUID.randomUUID().toString());

        // Then
        verify(slackClient).findSlackIdByEmail("user@team.com");
        verify(notificationSaver).saveAndPublish(any(), eq("U_NEW_ID"), any());
    }

    @Test
    @DisplayName("알림 삭제 로직: 권한 유저의 ID가 정상적으로 마스킹/변환되는지 확인")
    void deleteNotificationTest() {
        // Given
        UUID notiId = UUID.randomUUID();
        Notification mockNoti = spy(Notification.builder().msgContent("삭제될 메시지").build());
        given(notificationRepository.findByIdAndDeletedAtIsNull(notiId)).willReturn(java.util.Optional.of(mockNoti));

        // When
        notificationService.deleteNotification(notiId, "MASTER_ADMIN_USER_ID");

        // Then
        verify(mockNoti).delete(any(UUID.class));
        verify(notificationRepository).save(mockNoti);
    }
}
