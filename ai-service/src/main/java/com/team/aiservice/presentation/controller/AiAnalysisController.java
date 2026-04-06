package com.team.aiservice.presentation.controller;

import com.team.aiservice.application.dto.AiAnalysisResponse;
import com.team.aiservice.application.service.AiAnalysisService;
import com.team.aiservice.domain.model.AiAnalysis;
import com.team.common.ApiResponse;
import com.team.common.page.PageResponse;
import com.team.common.page.PageSizeUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ais")
@RequiredArgsConstructor
public class AiAnalysisController {
    private final AiAnalysisService aiAnalysisService;

    @GetMapping
    public ApiResponse<PageResponse<AiAnalysisResponse>> getAnalyses(
        @RequestParam(required = false) UUID orderId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {

        int normalizedSize = PageSizeUtils.normalize(size);
        Pageable pageable = PageRequest.of(page - 1, normalizedSize, Sort.by("createdAt").descending());

        Page<AiAnalysis> result = aiAnalysisService.search(orderId, pageable);
        return ApiResponse.success(
            PageResponse.from(result.map(a -> new AiAnalysisResponse(a.getId(), a.getAiResult()))));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        aiAnalysisService.softDelete(id, userId);
        return ApiResponse.ok();
    }
}
