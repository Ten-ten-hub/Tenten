package com.team.authservice.auth.infrastructure.feign;

import com.team.authservice.auth.infrastructure.feign.dto.UserRoleRes;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyReq;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyRes;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserInternalClient {

    @PostMapping("/internal/v1/users/verify")
    UserVerifyRes verify(@RequestBody UserVerifyReq userVerifyReqDto);

    @GetMapping("/internal/v1/users/{userId}/role")
    UserRoleRes getUserRole(@PathVariable UUID userId);
}
