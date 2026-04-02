package com.team.deliveryservice.infrastructure.client;

import com.team.deliveryservice.global.config.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
    name = "hub-service",
    configuration = FeignRetryConfig.class
)
public interface HubClient {
}
