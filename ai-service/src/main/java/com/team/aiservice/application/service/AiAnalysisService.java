package com.team.aiservice.application.service;

import com.team.aiservice.application.dto.AiRequest;
import com.team.aiservice.domain.model.AiAnalysis;
import com.team.aiservice.domain.model.AnalysisType;
import com.team.aiservice.domain.repository.AiAnalysisRepository;
import com.team.aiservice.infrastructure.client.HubClient;
import com.team.aiservice.infrastructure.client.NotificationClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
//@RequiredArgsConstructor //TODO mock 제거 후 활성화
public class AiAnalysisService {

    private static final Pattern SCHEDULE_TIME_PATTERN = Pattern.compile("\\[TIME: (.*?)\\]");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OpenAiChatModel chatModel;
    @Qualifier("mockHubClient") //TODO 임시
    private final HubClient hubClient;
    private final NaverNewsService naverNewsService;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final NotificationClient notificationClient;

    // 생성자를 직접 작성하여 mockHubClient를 주입받도록 지정 TODO mock 제거 후 지우기
    public AiAnalysisService(
        OpenAiChatModel chatModel,
        @Qualifier("mockHubClient") HubClient hubClient, // 명시적 지정
        NaverNewsService naverNewsService,
        AiAnalysisRepository aiAnalysisRepository,
        NotificationClient notificationClient
    ) {
        this.chatModel = chatModel;
        this.hubClient = hubClient;
        this.naverNewsService = naverNewsService;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public AiAnalysis analyzeDeadline(AiRequest request) {

        LocalDateTime now = LocalDateTime.now();
        String currentTimeStr = now.format(DATE_TIME_FORMATTER);

        // 1. 허브 경로 소요시간 조회 (외부 호출)
        var route = hubClient.getRoute(request.originHubId(), request.destinationHubId());

        if (route == null || route.duration() == null) {
            log.error("[HUB CLIENT ERROR] 경로 정보를 가져올 수 없습니다. Origin: {}, Dest: {}",
                request.originHubId(), request.destinationHubId());
            throw new RuntimeException("배송 경로 정보(소요 시간)가 유효하지 않아 AI 분석이 불가능합니다.");
        }

        // 2. 광역 뉴스 검색 (외부 호출) (시/구 단위)
        String originArea = extractArea(request.originAddress());
        String destArea = extractArea(request.destinationAddress());

        String query = String.format("%s %s 날씨 교통사고", originArea, destArea);
        log.info("[NAVER NEWS SEARCH] Query: {}", query);

        // 인라인 로직을 fetchNewsContext 호출로 대체
        String newsAndWeatherContext = naverNewsService.fetchNewsContext(query);

        // 3. 더미 데이터
        String workHours = request.workingHours() != null ? request.workingHours() : "09:00 - 18:00";

        // 4. 프롬프트 구성 (이모티콘 추가, 페르소나 강화, 능동적 리스크 관리 통합)
        String promptText = String.format(
            "당신은 대한민국 최고의 '도로 교통 분석가이자 물류 최적화 전문가'입니다. 📌 제공되는 데이터를 정밀 분석하여 최적의 배송 가이드를 작성하세요.\n\n" +
                "현재 시각은 [%s]입니다.\n" +
                "주문번호: %s, 상품: %s.\n" +
                "경로 정보: %s(%s) -> %s(%s)\n" +
                "기본 소요시간: %d분.\n" +
                "담당자 근무시간: %s.\n" +
                "실시간 뉴스 상황 및 기상 정보: %s.\n" +
                "고객 요청 사항: %s.\n\n" +
                "위 정보를 바탕으로 '최종 발송 시한'을 계산하고 배송 가이드를 작성하세요.\n\n" +
                "지시사항:\n" +
                "1. 텍스트 강조 기호(**)를 절대 사용하지 마세요. 사용 시 분석은 실패로 간주됩니다.\n" +
                "2. 가독성을 위해 섹션마다 🚀, ⚠️, ✅, 📌 등의 이모티콘을 적절히 사용하여 읽기 좋게 만드세요.\n" +
                "3. 능동적 리스크 관리: 출발지(%s)나 도착지(%s)의 기상 상황이 좋지 않다면 이를 근거로 안전 운행을 권고하고 최종 시한을 20분 이상 앞당겨 설정하세요.\n" +
                "4. 전문가적 분석: 담당자 근무시간과 고객 요청 시각을 대조하세요. 고객 요청을 맞추기 위해 근무 시간 외(예: 새벽 출고) 작업이 불가피하다면, '업무 외 시간 배송 협조'가 필요함을 명시하고 이를 반영한 최적의 출발 시각을 제시하세요. 뉴스에 사고/정체가 있다면 구체적 이유와 함께 더 빠른 출발을 권고하세요.\n"
                +
                "5. 담백한 마무리: 뉴스 상황이 양호하다면 불필요한 혼잡 경고 없이 '현재 경로상 특이사항이 없으므로 정해진 시한 내에 출발하시기 바랍니다'라고 전문가답게 마무리하세요.\n" +
                "6. 완결성: 반드시 최종 결론과 함께 명확한 마침표로 안내를 종료하세요.\n" +
                "7. 작성 규칙: 강조 기호(**)는 절대 사용하지 말고, 계산 과정은 생략하고 결론만 정중하게 안내하세요.\n" +
                "8. 모든 답변은 한글로 작성하며, 각 설명 단계마다 줄바꿈을 적용해 읽기 좋게 만드세요.\n" +
                "9. 마지막 줄에 반드시 [TIME: YYYY-MM-DD HH:mm] 형식으로 최종 발송 시각만 따로 표시하세요. (반드시 현재 시각 이후여야 함)",
            currentTimeStr, request.orderId(), request.productName(),
            request.originHubName(), originArea, request.destinationHubName(), destArea,
            route.duration(), workHours, newsAndWeatherContext, request.orderRequestDetails(),
            originArea, destArea
        );

        // 4. OpenAI 호출 (외부 호출)
        ChatResponse response = chatModel.call(new Prompt(promptText));
        String rawResult = response.getResult().getOutput().getText().replace("\\n", "\n").replace("**", "");

        // 5. DB 저장
        String cleanResult = rawResult.replaceAll("\\[TIME:.*?\\]", "").trim();
        AiAnalysis savedAnalysis = aiAnalysisRepository.save(AiAnalysis.builder()
            .orderId(request.orderId())
            .analysisType(AnalysisType.DEADLINE)
            .inputData(Map.of(
                "duration", route.duration(),
                "news_weather", newsAndWeatherContext,
                "currentTime", currentTimeStr
            ))
            .outputData(Map.of("ai_raw_res", rawResult))
            .aiResult(cleanResult)
            .build());

        // 6. 알림 서비스 호출 (외부 호출)
        // savedAnalysis.getId()가 확실히 생성된 후 호출
        LocalDateTime scheduledAt = parseScheduledTime(rawResult);
        try {
            notificationClient.sendWithAi(new NotificationClient.AiNotificationRequest(
                request.orderId(),
                request.receiverId(),
                request.receiverSlackId(),
                cleanResult,
                scheduledAt,
                savedAnalysis.getId()
            ), "ORDER_ALERT");
        } catch (Exception e) {
            log.error("Notification Service call failed: {}", e.getMessage());
        }

        return savedAnalysis;
    }

    private String extractArea(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        String[] parts = address.split(" ");
        // "서울특별시 송파구" -> "서울특별시" 만 추출하여 검색 확률을 높임
        return parts[0];
    }

    // 시간 파싱 유틸리티
    private LocalDateTime parseScheduledTime(String text) {
        try {
            Matcher matcher = SCHEDULE_TIME_PATTERN.matcher(text); // 상수 사용
            if (matcher.find()) {
                return LocalDateTime.parse(matcher.group(1).trim(), DATE_TIME_FORMATTER); // 상수 사용
            }
        } catch (Exception e) {
            log.warn("Failed to parse time from AI result: {}", e.getMessage());
        }
        return LocalDateTime.now().plusHours(2);
    }
}
