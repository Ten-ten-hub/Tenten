package com.team.userservice.user.users.application;

import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import com.team.userservice.user.users.application.dto.SignUpServiceDto;
import java.util.UUID;

public interface UserService {
    SignUpResultDto signUp(SignUpServiceDto serviceDto);

    void register(UUID userId, Role giveRole);

    LoginServiceDto loginService(String loginId, String password);
}
