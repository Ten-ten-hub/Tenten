package com.team.aiservice.application.service;

import com.team.aiservice.application.dto.AiRequest;
import com.team.aiservice.domain.model.AiAnalysis;
import com.team.aiservice.domain.model.AnalysisType;
import com.team.aiservice.domain.repository.AiAnalysisRepository;
import com.team.aiservice.infrastructure.client.NotificationClient;
import com.team.aiservice.presentation.common.ErrorCode;
import com.team.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private static final Pattern SCHEDULE_TIME_PATTERN = Pattern.compile("\\[TIME: (.*?)\\]");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OpenAiChatModel chatModel;
    private final NaverNewsService naverNewsService;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final HubRouteCacheService hubRouteCacheService;
    private final StreamBridge streamBridge;

    @Transactional
    public AiAnalysis analyzeDeadline(AiRequest request) {

        LocalDateTime now = LocalDateTime.now();
        String currentTimeStr = now.format(DATE_TIME_FORMATTER);

        // 1. 허브 경로 소요시간 조회 (외부 호출)
        var route = hubRouteCacheService.getCachedRoute(request.originHubId(), request.destinationHubId());

        if (route == null || route.duration() == null) {
            log.error("[HUB ERROR] 유효하지 않은 경로 정보");
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT_VALUE);
        }

        // 2. 광역 뉴스 검색 쿼리 최적화
        String originArea = extractArea(request.originAddress());
        String destArea = extractArea(request.destinationAddress());

        // 현재 날짜를 쿼리에 포함하여 최신 정보를 강제함
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
        String query = String.format("%s %s %s 실시간 날씨 교통사고", originArea, destArea, today);
        log.info("[NAVER NEWS SEARCH] Optimized Query: {}", query);

        // 인라인 로직을 fetchNewsContext 호출로 대체
        String newsAndWeatherContext = naverNewsService.fetchNewsContext(query);

        // 3. 더미 데이터
        String workHours = request.workingHours() != null ? request.workingHours() : "09:00 - 18:00";

// 4. 프롬프트 구성
        String promptText = String.format(
            "당신은 대한민국 최고의 '물류 운영 최적화 전문가'입니다. 아래 데이터를 바탕으로 오차 없는 배송 실행 시한을 도출하세요.\n\n" +
                "현재 시각은 [%s]입니다.\n" +
                "주문번호: %s, 상품: %s.\n" +
                "경로 정보: %s(%s) -> %s(%s)\n" +
                "기본 소요시간: %d분.\n" +
                "담당자 근무시간: %s.\n" +
                "현지 상황(뉴스/기상): %s.\n" +
                "고객 요청 사항: %s.\n\n" +
                "위 정보를 바탕으로 '최종 발송 시한'을 계산하고 배송 가이드를 작성하세요.\n\n" +
                "지시사항:\n" +
                "1. 최상단에 반드시 다음 정보를 인용구 형식(각 줄 앞에 '>' 사용)으로 포함하세요. \n" +
                "   > 주문 번호 : %s\n" +
                "   > 상품 정보 : %s\n" +
                "   > 요청 사항 : %s\n" +
                "   > 발송지 : %s\n" +
                "   > 도착지 : %s\n\n" +
                "2. 위 요약 정보를 본문에서 중복 나열하지 마세요. 텍스트 강조 기호(**) 사용을 엄격히 금지합니다.\n" +
                "3. 가독성을 위해 섹션마다 🚀, ⚠️, ✅, 📌 등의 이모티콘을 적절히 사용하여 섹션을 구분하세요.\n" +
                "4. 능동적 리스크 관리: 출발지(%s)나 도착지(%s)의 기상 상황이 좋지 않거나 뉴스에 사고/정체가 있다면 이를 구체적 근거로 안전 운행을 권고하고 최종 시한을 20분 이상 앞당겨 설정하세요.\n"
                +
                "5. 전문가적 분석: 담당자 근무시간과 고객 요청 시각을 대조하세요. 고객 요청을 맞추기 위해 근무 시간 외(예: 새벽 출고) 작업이 불가피하다면, '업무 외 시간 배송 협조'가 필요함을 명시하고 이를 반영한 최적의 출발 시각을 제시하세요.\n"
                +
                "6. 담백한 마무리: 뉴스 상황이 양호하다면 불필요한 혼잡 경고 없이 '현재 경로상 특이사항이 없으므로 정해진 시한 내에 출발하시기 바랍니다'라고 전문가답게 마무리하세요.\n" +
                "7. 완결성: 반드시 '위 내용을 기반으로 도출된 최종 발송 시한은 YYYY-MM-DD HH:mm 입니다.'라는 결론과 함께 명확한 마침표로 안내를 종료하세요.\n" +
                "8. 작성 규칙: 강조 기호(**)는 절대 사용하지 말고, 계산 과정은 생략하고 결론만 정중하게 안내하세요.\n" +
                "9. 모든 답변은 한글로 작성하며, 각 설명 단계마다 줄바꿈을 적용해 읽기 좋게 만드세요.\n" +
                "10. **중요 로직 - 발송 시한 산출 방식 (24시간제 기준)**:\n" +
                "    - 단계 1: (고객 요청 도착 시각) - (배송 소요시간 %d분)을 계산하여 '출발 한계 시각'을 산출하세요.\n" +
                "    - 단계 2: 산출된 '출발 한계 시각'이 현재 시각[%s]보다 과거라면 오늘 배송은 물리적으로 불가능하므로, 날짜를 '내일(익일)'로 변경하여 재계산하세요.\n" +
                "    - 단계 3: **수치 비교 절대 원칙**: 담당자 근무 시작 시각인 09:00보다 산출된 시각이 크다면(예: 10:00, 11:00), 이는 근무 시작 '이후'이므로 절대로 09:00로 시간을 앞당기지 마세요. 계산된 시각(10:00)을 최종 시한으로 확정하세요.\n"
                +
                "    - 단계 4: 산출된 시각이 09:00보다 작은 숫자(예: 08:00)일 때만 근무 시작 전으로 판단하여 '업무 외 협조'를 요청하세요.\n" +
                "    - 결과 설명 시에는 '오전/오후' 표현을 섞되, 결론 날짜와 시간 형식은 정확히 유지하세요.\n" +
                "11. 마지막 줄에 [TIME: YYYY-MM-DD HH:mm] 형식을 반드시 유지하세요. 시각은 무조건 현재 시각[%s] 이후여야 합니다.",

            currentTimeStr, request.orderId(), request.productName(),
            request.originHubName(), originArea, request.destinationHubName(), destArea,
            route.duration(), workHours, newsAndWeatherContext, request.orderRequestDetails(),
            request.orderId(), request.productName(), request.orderRequestDetails(), request.originHubName(),
            request.destinationAddress(),
            originArea, destArea,
            route.duration(), currentTimeStr, currentTimeStr
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
        sendNotification(request, rawResult, savedAnalysis);

        return savedAnalysis;
    }

    /**
     * 알림 서비스 호출 및 도메인 매핑 (AnalysisType -> MsgType)
     */
    private void sendNotification(AiRequest request, String rawResult, AiAnalysis savedAnalysis) {
        LocalDateTime scheduledAt = parseScheduledTime(rawResult);
        String cleanResult = rawResult.replaceAll("\\[TIME:.*?\\]", "").trim();

        // 분석 타입에 따른 알림 서비스용 메시지 타입 결정
        String targetMsgType = determineMsgType(savedAnalysis.getAnalysisType());

        try {
            NotificationClient.AiNotificationRequest kafkaPayload = new NotificationClient.AiNotificationRequest(
                request.orderId(),
                request.receiverId(),
                request.receiverSlackId(),
                cleanResult,
                scheduledAt,
                savedAnalysis.getId(),
                targetMsgType
            );

            // application.properties에서 설정한 binding 이름: "ai-notification-out-0"
            streamBridge.send("ai-notification-out-0", kafkaPayload);
            log.info("Kafka 알림 메시지 발행 성공: AnalysisID={}", savedAnalysis.getId());
        } catch (Exception e) {
            log.error("Kafka 알림 메시지 발행 실패: {}", e.getMessage());
        }
    }

    /**
     * AnalysisType(AI 도메인)을 MsgType(알림 도메인 문자열)으로 매핑 StreamBridge를 사용하여 비동기 메시지 전송
     */
    private String determineMsgType(AnalysisType analysisType) {
        return switch (analysisType) {
            case DEADLINE -> "ORDER_ALERT";
            case ROUTE -> "DAILY_REPORT";
            default -> "ORDER_ALERT";
        };
    }

    private String extractArea(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        // 연속된 공백 및 앞뒤 공백 처리 후 시/도 단위 추출
        String trimmed = address.trim();
        String[] parts = trimmed.split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    private LocalDateTime parseScheduledTime(String text) {
        try {
            Matcher matcher = SCHEDULE_TIME_PATTERN.matcher(text);
            if (matcher.find()) {
                return LocalDateTime.parse(matcher.group(1).trim(), DATE_TIME_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to parse time from AI result: {}", e.getMessage());
        }
        return LocalDateTime.now().plusHours(2);
    }

    @Transactional(readOnly = true)
    public Page<AiAnalysis> search(UUID orderId, Pageable pageable) {
        if (orderId != null) {
            return aiAnalysisRepository.findByOrderIdAndDeletedAtIsNull(orderId, pageable);
        }
        return aiAnalysisRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Transactional
    public void softDelete(UUID id, UUID deletedBy) {
        AiAnalysis analysis = aiAnalysisRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.AI_NOT_FOUND));

        analysis.softDelete(deletedBy);
        aiAnalysisRepository.save(analysis);
    }

    /**
     * AI 분석 ID를 통한 단건 조회 (삭제된 데이터 제외)
     */
    @Transactional(readOnly = true)
    public AiAnalysis findById(UUID id) {
        return aiAnalysisRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.AI_NOT_FOUND));
    }
}
