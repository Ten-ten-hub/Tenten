package com.team.deliveryservice.infrastructure.client;

import com.team.deliveryservice.global.config.FeignAuthForwardConfig;
import com.team.deliveryservice.global.config.FeignRetryConfig;
import com.team.deliveryservice.infrastructure.client.dto.HubExistsResponse;
import com.team.deliveryservice.infrastructure.client.dto.OptimalRouteResponseWrapper;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "hub-service",
    configuration = {FeignRetryConfig.class, FeignAuthForwardConfig.class}
)
public interface HubClient {

    @GetMapping("/internal/v1/hubs/{hubId}/exists")
    HubExistsResponse existsHub(
        @PathVariable UUID hubId,
        @RequestHeader("X-Internal-Request") String internalHeader
    );

    @GetMapping("/internal/v1/hub-route/optimal")
    OptimalRouteResponseWrapper getOptimalRoute(
        @RequestParam UUID departureHubId,
        @RequestParam UUID arrivalHubId,
        @RequestHeader("X-Internal-Request") String internalHeader
    );
}
