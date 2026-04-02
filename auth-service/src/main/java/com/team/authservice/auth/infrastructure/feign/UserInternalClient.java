package com.team.authservice.auth.infrastructure.feign;

import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyReqDto;
import com.team.authservice.auth.infrastructure.feign.dto.UserVerifyResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserInternalClient {

    @PostMapping("/internal/v1/users/verify")
    UserVerifyResDto verify(@RequestBody UserVerifyReqDto userVerifyReqDto);
}
