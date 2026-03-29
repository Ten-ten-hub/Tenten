package com.team.userservice.user.users.application.dto;

import com.team.userservice.user.core.enums.Role;

public record SignUpServiceDto(
        String name,
        String loginId,
        String password,
        Role role,
        String slackId,
        String email,
        String phoneNumber
) {
}
