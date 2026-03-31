package com.team.userservice.user.users.presentation;

import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.request.LoginReqDto;
import com.team.userservice.user.users.presentation.dto.response.LoginResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    @PostMapping("/users/verify")
    public LoginResDto login(@RequestBody LoginReqDto loginRequest) {
        return LoginResDto.from(userService.loginService(loginRequest.loginId(), loginRequest.password()));
    }
}
