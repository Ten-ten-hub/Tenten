package com.team.aiservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team.aiservice.application.dto.AiRequest;
import com.team.aiservice.application.service.AiAnalysisService;
import com.team.aiservice.application.service.HubRouteCacheService;
import com.team.aiservice.application.service.NaverNewsService;
import com.team.aiservice.application.service.NotificationPublishedEvent;
import com.team.aiservice.domain.model.AiAnalysis;
import com.team.aiservice.domain.repository.AiAnalysisRepository;
import com.team.aiservice.infrastructure.client.HubClient;
import com.team.common.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AiServiceApplicationTests {

    @InjectMocks
    private AiAnalysisService aiAnalysisService;
    @Mock
    private OpenAiChatModel chatModel;
    @Mock
    private NaverNewsService naverNewsService;
    @Mock
    private HubRouteCacheService hubRouteCacheService;
    @Mock
    private AiAnalysisRepository aiAnalysisRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("성공: AI 분석 시나리오 - 데이터 수집, OpenAI 호출, 결과 파싱, 이벤트 발행 확인")
    void analyzeDeadline_FullSuccess() {
        // Given
        AiRequest request = createAiRequest();
        HubClient.HubRouteResponse route = new HubClient.HubRouteResponse(100, 50.0, 0.0, 0.0, 0.0, 0.0);
        given(hubRouteCacheService.getCachedRoute(any(), any())).willReturn(route);
        given(naverNewsService.fetchNewsContext(anyString())).willReturn("교통 원활");

        AssistantMessage assistantMessage = new AssistantMessage("분석 완료 [TIME: 2026-04-07 10:00]");
        given(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
            .willReturn(new ChatResponse(List.of(new Generation(assistantMessage))));

        given(aiAnalysisRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // When
        AiAnalysis result = aiAnalysisService.analyzeDeadline(request);

        // Then
        assertNotNull(result);
        assertEquals("분석 완료", result.getAiResult());
        verify(aiAnalysisRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(NotificationPublishedEvent.class));
    }

    @Test
    @DisplayName("실패: 허브 경로 정보가 없을 경우 예외 처리 확인")
    void analyzeDeadline_Fail_NoHubRoute() {
        given(hubRouteCacheService.getCachedRoute(any(), any())).willReturn(null);
        assertThrows(BusinessException.class, () -> aiAnalysisService.analyzeDeadline(createAiRequest()));
    }

    @Test
    @DisplayName("성공: 분석 기록 삭제 시 soft delete 로직 정상 작동 확인")
    void softDelete_LogicCheck() {
        UUID id = UUID.randomUUID();
        AiAnalysis analysis = spy(AiAnalysis.builder().id(id).aiResult("기록").build());
        given(aiAnalysisRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(analysis));

        aiAnalysisService.softDelete(id, UUID.randomUUID());

        verify(analysis).softDelete(any());
        verify(aiAnalysisRepository).save(analysis);
    }

    private AiRequest createAiRequest() {
        return new AiRequest(UUID.randomUUID(), "냉장고", UUID.randomUUID(), "서울허브", "서울",
            UUID.randomUUID(), "부산허브", "부산", "배송주의", UUID.randomUUID(), "slack-id", "09:00-18:00");
    }
}
