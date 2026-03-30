package com.team.companyservice.infrastructure.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.team.companyservice.infrastructure.config.FeignRetryConfig;

@FeignClient(
        name = "${client.hub-service.name}",
        url = "${client.hub-service.url}",
        configuration = FeignRetryConfig.class
)
public interface HubClient {

    @GetMapping("/internal/v1/hubs/{hubId}/exists")
    HubExistsResponse existsHub(@PathVariable UUID hubId);
}