package com.team.userservice.user.users.presentation;

import com.team.userservice.global.dto.CommonResponse;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.request.LoginReq;
import com.team.userservice.user.users.presentation.dto.response.GetAllUserInfoRes;
import com.team.userservice.user.users.presentation.dto.response.GetRoleRes;
import com.team.userservice.user.users.presentation.dto.response.GetUserInfoRes;
import com.team.userservice.user.users.presentation.dto.response.LoginRes;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserAffiliationReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserRoleReq;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    @PostMapping("/v1/users/verify")
    public LoginRes login(@Valid @RequestBody LoginReq loginRequest) {
        return LoginRes.from(userService.loginService(loginRequest.loginId(), loginRequest.password()));
    }
// 사용자 정보 다건 조회 내부 api
    @GetMapping("/v1/users")
    public CommonResponse<List<GetAllUserInfoRes>> getAllUserInfoInternal(
        @RequestParam(required = false) List<Role> roles,
        @RequestParam(required = false) AffiliatedStatus affiliatedStatus
    ) {
        return CommonResponse.onSuccess(
            userService.getAllUserInfoInternal(roles, affiliatedStatus)
                .stream()
                .map(GetAllUserInfoRes::from)
                .toList()
        );
    }

    //사용자 정보 단건 조회 내부 api
    @GetMapping("/v1/users/{userId}")
    public CommonResponse<GetUserInfoRes> getUserInfoInternal(@PathVariable UUID userId) {
        return CommonResponse.onSuccess(GetUserInfoRes.fromUserDataDto(userService.getUserInfo(userId)));
    }

    //마지막 로그인 일자 및 시각 업데이트 내부 api
    @PatchMapping("/v1/users/{userId}/last-login")
    public CommonResponse<Void> updateLastLoginAt(@PathVariable UUID userId) {
        userService.updateLastLoginAt(userId);
        return CommonResponse.onSuccess();
    }

    @GetMapping("/v1/users/{userId}/role")
    public CommonResponse<GetRoleRes> getUserRoleInternal(@PathVariable("userId") UUID userId) {
        return CommonResponse.onSuccess(new GetRoleRes(userService.getUserRole(userId)));
    }

    @PatchMapping("/v1/users/{userId}/role")
    public CommonResponse<Void> updateUserRoleInternal(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserRoleReq request
    ) {
        userService.updateUserRole(userId, request.role());
        return CommonResponse.onSuccess();
    }

    @PatchMapping("/v1/users/{userId}/affiliation")
    public CommonResponse<Void> updateUserAffiliationInternal(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserAffiliationReq request
    ) {
        userService.updateUserAffiliation(userId, request.affiliation(), request.affiliationId());
        return CommonResponse.onSuccess();
    }
}
