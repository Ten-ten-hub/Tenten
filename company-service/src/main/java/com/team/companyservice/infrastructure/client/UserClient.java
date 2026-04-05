package com.team.companyservice.infrastructure.client;

import com.team.companyservice.global.config.FeignRetryConfig;
import com.team.companyservice.infrastructure.client.dto.AffiliatedStatus;
import com.team.companyservice.infrastructure.client.dto.Role;
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
    configuration = FeignRetryConfig.class
)
public interface UserClient {

    @GetMapping("/internal/v1/users/{userId}")
    UserCommonResponse<UserInternalResponse> getUserInfo(@PathVariable UUID userId);

    @GetMapping("/internal/v1/users")
    UserCommonResponse<List<UserInternalResponse>> getUsers(
        @RequestParam(required = false) List<Role> roles,
        @RequestParam(required = false) AffiliatedStatus affiliatedStatus
    );

    @PatchMapping("/internal/v1/users/{userId}/role")
    UserCommonResponse<Void> updateUserRole(
        @PathVariable UUID userId,
        @RequestBody UpdateUserRoleRequest request
    );

    @PatchMapping("/internal/v1/users/{userId}/affiliation")
    UserCommonResponse<Void> updateUserAffiliation(
        @PathVariable UUID userId,
        @RequestBody UpdateUserAffiliationRequest request
    );
}
