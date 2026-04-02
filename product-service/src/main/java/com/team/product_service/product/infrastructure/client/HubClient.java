package com.team.product_service.product.infrastructure.client;

import com.team.product_service.product.infrastructure.client.dto.HubExistsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/internal/v1/hubs/{hubId}/exists")
    HubExistsResponse checkHubExists(
        @RequestHeader("X-Internal-Request") String internalRequest,
        @PathVariable UUID hubId
    );
}
