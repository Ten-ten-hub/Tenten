package com.team.userservice.user.users.application.dto;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record SignUpResultDto(
        UUID userId,
        String loginId,
        String name,
        Role role,
        String email,
        SignupStatus signupStatus,
        LocalDateTime createdAt
) {
    public static SignUpResultDto from(User user) {
        return new SignUpResultDto(
                user.getId(),
                user.getLoginId(), user.getName(),
                user.getRole(),
                user.getEmail(),
                user.getSignupStatus(),
                user.getCreatedAt()
        );
    }
}

