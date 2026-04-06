package com.team.notificationservice.infrastructure;

import com.team.notificationservice.application.AiNotificationRequest;
import com.team.notificationservice.infrastructure.config.FeignHeaderConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ai-service", configuration = FeignHeaderConfig.class)
public interface AiClient {

    @PostMapping("/internal/v1/ais/analysis")
    AiAnalysisResponse getAnalysis(
        @RequestParam("analysisType") String analysisType,
        @RequestBody AiNotificationRequest request
    );

    record AiAnalysisResponse(
        UUID aiAnalysisId,
        String aiResult // AI가 생성한 "최종 발송 시한은 12월 10일 오전 9시 입니다" 포함 문구
    ) {
    }
}
