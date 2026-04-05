package com.team.aiservice.presentation.controller;

import com.team.aiservice.application.dto.AiAnalysisResponse;
import com.team.aiservice.application.dto.AiRequest;
import com.team.aiservice.application.service.AiAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/internal/v1/ai")
@RequiredArgsConstructor
public class InternalAiController {
    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analysis")
    public AiAnalysisResponse analyze(@Valid @RequestBody AiRequest request) {
        try {
            // 서비스 내부에서 혹은 Feign/OpenAI 설정에서 타임아웃이 발생할 수 있음
            var result = aiAnalysisService.analyzeDeadline(request);
            return new AiAnalysisResponse(result.getId(), result.getAiResult());
        } catch (Exception e) {
            // 타임아웃 또는 외부 서비스 장애 발생 시 제어된 에러 응답
            log.error("AI Analysis failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서비스가 지연되거나 응답하지 않습니다.");
        }
    }
}
