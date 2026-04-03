package com.team.userservice.user.users.presentation.dto.request;

import com.team.userservice.user.core.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleReq(
    @NotNull Role role
) {
}
