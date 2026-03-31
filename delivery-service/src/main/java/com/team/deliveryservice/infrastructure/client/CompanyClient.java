package com.team.deliveryservice.infrastructure.client;

import com.team.deliveryservice.infrastructure.config.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
    name = "company-service",
    configuration = FeignRetryConfig.class
)
public interface CompanyClient {
}
