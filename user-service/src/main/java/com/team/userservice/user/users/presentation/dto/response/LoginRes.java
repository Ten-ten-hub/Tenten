package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import java.util.UUID;

public record LoginRes(
    UUID userId,
    Role role
) {
    public static LoginRes from(LoginServiceDto serviceDto) {
        return new LoginRes(
            serviceDto.uuid(),
            serviceDto.role()
        );
    }
}
