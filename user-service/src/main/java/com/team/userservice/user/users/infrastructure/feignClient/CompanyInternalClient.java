package com.team.userservice.user.users.infrastructure.feignClient;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "company-service")
public interface CompanyInternalClient {

    @GetMapping("/{companyId}/exists")
    void isValidID(@PathVariable("companyId") UUID affiliationId);
}
