package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import java.util.UUID;

public record LoginResDto(
        UUID userId,
        Role role
) {
    public static LoginResDto from(LoginServiceDto serviceDto) {
        return new LoginResDto(
                serviceDto.uuid(),
                serviceDto.role()
        );
    }
}
