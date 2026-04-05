package com.team.companyservice.infrastructure.client;

import com.team.companyservice.global.config.FeignConfig;
import com.team.companyservice.global.config.FeignRetryConfig;
import com.team.companyservice.infrastructure.client.dto.UpdateUserAffiliationRequest;
import com.team.companyservice.infrastructure.client.dto.UpdateUserRoleRequest;
import com.team.companyservice.infrastructure.client.dto.UserCommonResponse;
import com.team.companyservice.infrastructure.client.dto.UserInternalResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "${client.user-service.name}",
    url = "${client.user-service.url}",
    configuration = {FeignRetryConfig.class, FeignConfig.class}
)
public interface UserClient {

    /**
     * 사용자 단건 내부 조회
     */
    @GetMapping("/internal/v1/users/{userId}")
    UserCommonResponse<UserInternalResponse> getUserInfo(@PathVariable UUID userId);

    /**
     * 사용자 다건 내부 조회
     */
    @GetMapping("/internal/v1/users")
    UserCommonResponse<List<UserInternalResponse>> getUsers(
        @RequestParam(required = false) List<String> roles,
        @RequestParam(required = false) String affiliatedStatus
    );

    /**
     * 사용자 권한 변경
     */
    @PatchMapping("/internal/v1/users/{userId}/role")
    UserCommonResponse<Void> updateUserRole(
        @PathVariable UUID userId,
        @RequestBody UpdateUserRoleRequest request
    );

    /**
     * 사용자 소속 변경
     */
    @PatchMapping("/internal/v1/users/{userId}/affiliation")
    UserCommonResponse<Void> updateUserAffiliation(
        @PathVariable UUID userId,
        @RequestBody UpdateUserAffiliationRequest request
    );
}
