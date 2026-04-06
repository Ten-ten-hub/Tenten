package com.team.aiservice.application.dto;

import java.util.UUID;

public record AiAnalysisResponse(
    UUID aiAnalysisId,
    String aiResult
) {
}
