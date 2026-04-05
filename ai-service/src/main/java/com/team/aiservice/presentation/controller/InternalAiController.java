package com.team.aiservice.presentation.controller;

import com.team.aiservice.application.dto.AiAnalysisResponse;
import com.team.aiservice.application.dto.AiRequest;
import com.team.aiservice.application.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/ai")
@RequiredArgsConstructor
public class InternalAiController {
    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analysis")
    public AiAnalysisResponse analyze(@RequestBody AiRequest request) {
        var result = aiAnalysisService.analyzeDeadline(request);
        return new AiAnalysisResponse(result.getId(), result.getAiResult());
    }
}
