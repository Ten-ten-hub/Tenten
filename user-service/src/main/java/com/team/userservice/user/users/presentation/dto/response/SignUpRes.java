package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import java.time.LocalDateTime;
import java.util.UUID;

public record SignUpRes(
    UUID userId,
    String loginId,
    String name,
    Role role,
    String email,
    SignupStatus signupStatus,
    LocalDateTime createdAt
) {
    public static SignUpRes from(SignUpResultDto result) {
        return new SignUpRes(
            result.userId(),
            result.loginId(),
            result.name(),
            result.role(),
            result.email(),
            result.signupStatus(),
            result.createdAt()
        );
    }
}

