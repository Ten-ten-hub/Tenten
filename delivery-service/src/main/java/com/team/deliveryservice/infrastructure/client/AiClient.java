package com.team.deliveryservice.infrastructure.client;

import com.team.deliveryservice.infrastructure.client.dto.AiRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ai-service")
public interface AiClient {
    @PostMapping("/internal/v1/ais/analysis")
    void triggerAiAnalysis(
        @RequestHeader("X-Internal-Request") String internal,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-User-Role") String role,
        @RequestBody AiRequest request
    );
}
