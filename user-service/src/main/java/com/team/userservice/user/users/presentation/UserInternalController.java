package com.team.userservice.user.users.presentation;

import com.team.userservice.global.dto.CommonResponse;
import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.response.LoginResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    @GetMapping("/users/login-id/{loginId}")
    public CommonResponse<LoginResDto> login(@PathVariable("loginId") String loginId) {
        return CommonResponse.onSuccess(LoginResDto.from(userService.findByLoginId(loginId)));
    }
}
