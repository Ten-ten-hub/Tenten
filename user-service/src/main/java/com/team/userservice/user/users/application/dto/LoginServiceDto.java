package com.team.userservice.user.users.application.dto;

import com.team.userservice.user.core.enums.Role;
import java.util.UUID;

public record LoginServiceDto(
        UUID uuid,
        Role role
) {
    public static LoginServiceDto from(UUID userId, Role role) {
        return new LoginServiceDto(userId, role);
    }
}
