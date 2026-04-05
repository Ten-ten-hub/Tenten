package com.team.userservice.user.users.presentation;

import com.team.userservice.global.dto.CommonResponse;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.request.LoginReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserAffiliationReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserRoleReq;
import com.team.userservice.user.users.presentation.dto.response.GetRoleRes;
import com.team.userservice.user.users.presentation.dto.response.InternalUserInfoRes;
import com.team.userservice.user.users.presentation.dto.response.InternalUserSummaryRes;
import com.team.userservice.user.users.presentation.dto.response.LoginRes;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 서비스 간 연동용 컨트롤러
 * 외부 API 응답과 분리된 내부 계약 DTO를 사용한다.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    @PostMapping("/v1/users/verify")
    public LoginRes login(@Valid @RequestBody LoginReq loginRequest) {
        return LoginRes.from(userService.loginService(loginRequest.loginId(), loginRequest.password()));
    }

    /**
     * 내부 사용자 다건 조회
     * company-service 등에서 업체 관리자 중복 지정 여부 확인 시 사용
     */
    @GetMapping("/v1/users")
    public CommonResponse<List<InternalUserSummaryRes>> getAllUserInfoInternal(
        @RequestParam(required = false) List<Role> roles,
        @RequestParam(required = false) AffiliatedStatus affiliatedStatus
    ) {
        return CommonResponse.onSuccess(
            userService.getAllUserInfoInternal(roles, affiliatedStatus)
                .stream()
                .map(InternalUserSummaryRes::from)
                .toList()
        );
    }

    /**
     * 내부 사용자 단건 조회
     * company-service에서 업체 관리자 지정 시 사용
     */
    @GetMapping("/v1/users/{userId}")
    public CommonResponse<InternalUserInfoRes> getUserInfoInternal(@PathVariable UUID userId) {
        return CommonResponse.onSuccess(
            InternalUserInfoRes.from(userService.getUserInfo(userId))
        );
    }

    /**
     * 마지막 로그인 시각 업데이트
     */
    @PatchMapping("/v1/users/{userId}/last-login")
    public CommonResponse<Void> updateLastLoginAt(@PathVariable UUID userId) {
        userService.updateLastLoginAt(userId);
        return CommonResponse.onSuccess();
    }

    /**
     * 내부 권한 조회
     */
    @GetMapping("/v1/users/{userId}/role")
    public CommonResponse<GetRoleRes> getUserRoleInternal(@PathVariable("userId") UUID userId) {
        return CommonResponse.onSuccess(new GetRoleRes(userService.getUserRole(userId)));
    }

    /**
     * 내부 권한 변경
     */
    @PatchMapping("/v1/users/{userId}/role")
    public CommonResponse<Void> updateUserRoleInternal(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserRoleReq request
    ) {
        userService.updateUserRole(userId, request.role());
        return CommonResponse.onSuccess();
    }

    /**
     * 내부 소속 변경
     */
    @PatchMapping("/v1/users/{userId}/affiliation")
    public CommonResponse<Void> updateUserAffiliationInternal(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserAffiliationReq request
    ) {
        userService.updateUserAffiliation(userId, request.affiliation(), request.affiliationId());
        return CommonResponse.onSuccess();
    }
}
