package com.team.hubservice.hub.infrastructure.client;

import com.team.hubservice.hub.infrastructure.client.dto.CompanyExistsResponse;
import com.team.hubservice.hub.infrastructure.config.FeignConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "company-service",
    configuration = FeignConfig.class
    //fallbackFactory = CompanyClientFallbackFactory.class
)
public interface CompanyClient {

    @GetMapping("/internal/v1/companies/exists")
    CompanyExistsResponse checkCompanyExistsByHubId(@RequestParam("hubId") UUID hubId);
}
