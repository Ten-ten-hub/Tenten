package com.team.userservice.user.users.infrastructure.feignClient;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hub-service")
public interface HubInternalClient {

    @GetMapping("/{hubId}/exists/v2")
    void isValidID(@PathVariable("hubId") UUID affiliationId);
}
