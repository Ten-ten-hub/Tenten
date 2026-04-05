package com.team.userservice.user.users.presentation;

import com.team.userservice.global.auth.RequireRole;
import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.dto.CommonResponse;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.request.SignUpReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserAffiliationReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserRoleReq;
import com.team.userservice.user.users.presentation.dto.response.GetAllUserInfoRes;
import com.team.userservice.user.users.presentation.dto.response.GetUserInfoRes;
import com.team.userservice.user.users.presentation.dto.response.SignUpRes;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 회원 가입 요청
    @PostMapping("/signup") // 비회원 : 외부 -> 게이트웨이 -> 인증x -> user-service
    public CommonResponse<SignUpRes> signUp(@Valid @RequestBody SignUpReq request) {

        return CommonResponse.onSuccess(HttpStatus.CREATED,
            "회원가입 요청이 완료되었습니다. 마스터의 승인을 기다려주세요.",
            SignUpRes.from(userService.signUp(request.toServiceDto())));
    }

    // 2. 회원 가입 승인
    @RequireRole(Role.MASTER_ADMIN)
    @PatchMapping("/{userId}/registration") // 마스터 관리자 : 외부 -> 게이트웨이 -> 인증o -> user-service
    public CommonResponse<String> register(@PathVariable("userId") UUID userId) {

        userService.register(userId);

        return CommonResponse.onSuccess("유저 등록 성공");
    }


    // 3. 단일 사용자 정보 수정
    @RequireRole(Role.MASTER_ADMIN)
    @PatchMapping("/{userId}")
    public CommonResponse<Void> updateUser(@PathVariable("userId") UUID userId,
                                           @Valid @RequestBody UpdateUserReq request) {
        userService.userUpdate(userId, request.toServiceDto());
        return CommonResponse.onSuccess();
    }


    // 4. 단일 사용자 권한 수정
    @RequireRole(Role.MASTER_ADMIN)
    @PatchMapping("/{userId}/role")
    public CommonResponse<Void> updateUserRole(@PathVariable("userId") UUID userId,
                                               @Valid @RequestBody UpdateUserRoleReq updateUserRoleReq) {
        userService.updateUserRole(userId, updateUserRoleReq.role());

        return CommonResponse.onSuccess();
    }

    // 5. 사용자 단건 조회
    @RequireRole(Role.MASTER_ADMIN)
    @GetMapping("/{userId}")
    public CommonResponse<GetUserInfoRes> getUserInfo(@PathVariable("userId") UUID userId) {
        return CommonResponse.onSuccess(GetUserInfoRes.fromUserDataDto(userService.getUserInfo(userId)));
    }

    // 6. 사용자 전체 조회
    @RequireRole(Role.MASTER_ADMIN)
    @GetMapping
    public CommonResponse<Page<GetAllUserInfoRes>> getAllUserInfo(
        @RequestParam(required = false) List<Role> roles,
        @RequestParam(required = false) AffiliatedStatus affiliatedStatus,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (roles == null && affiliatedStatus == null) {
            return CommonResponse.onSuccess(userService.getAllUserInfo(pageable).map(GetAllUserInfoRes::from));
        }
        return CommonResponse.onSuccess(userService.getAllUserInfo(roles, affiliatedStatus, pageable).map(GetAllUserInfoRes::from));
    }

    // 7. 사용자 삭제
    @RequireRole(Role.MASTER_ADMIN)
    @DeleteMapping("/{userId}")
    public CommonResponse<Void> deleteUser(@PathVariable("userId") UUID targetId,
                                           @RequestHeader("X-User-Id") UUID deletedBy) {
        userService.deleteUser(targetId, deletedBy);
        return CommonResponse.onSuccess();
    }

    // 8. 내 정보 수정
    @PatchMapping("/me")
    public CommonResponse<Void> updateMe(@Valid @RequestBody UpdateUserReq request,
                                         @RequestHeader("X-User-Id") UUID userId) {
        userService.userUpdate(userId, request.toServiceDto());
        return CommonResponse.onSuccess();
    }

    // 9. 내 정보 조회
    @GetMapping("/me")
    public CommonResponse<GetUserInfoRes> getMyInfo(@RequestHeader("X-User-Id") UUID userId) {
        return CommonResponse.onSuccess(GetUserInfoRes.fromUserDataDto(userService.getUserInfo(userId)));
    }

    // 10. 가입 승인 요청 목록 조회
    @GetMapping("/pending")
    @RequireRole(Role.MASTER_ADMIN)
    public CommonResponse<Page<GetAllUserInfoRes>> getPendingUserInfo(
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return CommonResponse.onSuccess(
            (userService.getAllUserInfoBySignUpStatus(SignupStatus.PENDING, pageable).map(GetAllUserInfoRes::from)));
    }

    // 11. 단일 사용자 소속 배정
    @RequireRole(Role.MASTER_ADMIN)
    @PatchMapping("/{userId}/affiliation")
    public CommonResponse<Void> updateUserAffiliation(@PathVariable("userId") UUID userId,
                                                      @Valid @RequestBody UpdateUserAffiliationReq request) {
        userService.updateUserAffiliation(userId, request.affiliation(), request.affiliationId());
        return CommonResponse.onSuccess();
    }

    // 12. 회원 탈퇴
}
