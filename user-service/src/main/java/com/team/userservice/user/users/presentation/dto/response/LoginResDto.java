package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.dto.LoginServiceDto;

public record LoginResDto(
        String loginId,
        String password,
        Role role
) {
    public static LoginResDto from(LoginServiceDto dto) {
        return new LoginResDto(
                dto.loginId(),
                dto.password(),
                dto.role()
        );
    }
}
