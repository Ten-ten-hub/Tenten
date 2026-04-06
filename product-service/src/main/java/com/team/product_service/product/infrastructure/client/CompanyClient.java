package com.team.product_service.product.infrastructure.client;

import com.team.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/internal/v1/companies/{companyId}/exists")
    ApiResponse<Void> checkCompanyExists(@PathVariable("companyId") UUID companyId);
}
