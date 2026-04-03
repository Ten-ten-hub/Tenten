package com.team.authservice.auth.infrastructure.feign.dto;

import com.team.authservice.core.enums.Role;

public record UserRoleRes(
    Role role
) {
}
