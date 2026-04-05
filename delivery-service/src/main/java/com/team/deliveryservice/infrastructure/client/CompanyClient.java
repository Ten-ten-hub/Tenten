package com.team.deliveryservice.infrastructure.client;

import com.team.deliveryservice.global.config.FeignAuthForwardConfig;
import com.team.deliveryservice.global.config.FeignRetryConfig;
import com.team.deliveryservice.infrastructure.client.dto.CompanyInternalResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "company-service",
    configuration = {FeignRetryConfig.class, FeignAuthForwardConfig.class}
)
public interface CompanyClient {

    @GetMapping("/internal/v1/companies/{companyId}")
    CompanyInternalResponse getCompany(@PathVariable UUID companyId);
}
