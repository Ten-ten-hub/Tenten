package com.team.product_service.product.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/internal/v1/hubs/{hubId}/exists/v2")
    void checkHubExists(@PathVariable UUID hubId);
}
