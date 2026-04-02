package com.team.deliveryservice.infrastructure.client;

import com.team.deliveryservice.global.config.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
    name = "order-service",
    configuration = FeignRetryConfig.class
)
public interface OrderClient {
}
