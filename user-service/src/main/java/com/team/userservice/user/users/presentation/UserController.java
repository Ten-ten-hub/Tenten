package com.team.userservice.user.users.presentation;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.dto.CommonResponse;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.request.RegisterReqDto;
import com.team.userservice.user.users.presentation.dto.request.SignUpReqDto;
import com.team.userservice.user.users.presentation.dto.response.SignUpResDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public CommonResponse<SignUpResDto> signUp(@Valid @RequestBody SignUpReqDto request) {

        return CommonResponse.onSuccess(HttpStatus.CREATED,
                "회원가입 요청이 완료되었습니다. 마스터의 승인을 기다려주세요.",
                SignUpResDto.from(userService.signUp(request.toServiceDto())));
    }

    @PatchMapping("/{userId}/registration")
    public CommonResponse<String> register(@PathVariable("userId") UUID userId,
                                           @RequestHeader("X-User-Role") Role role,
                                           @RequestBody RegisterReqDto request) {

        if (role != Role.MASTER_ADMIN) {
            throw new UserException(UserErrorCode.FORBIDDEN);
        }

        userService.register(userId, request.giveRole());

        return CommonResponse.onSuccess("유저 등록 성공");
    }
}
