package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import java.time.LocalDateTime;
import java.util.UUID;

public record SignUpResDto(
        UUID userId,
        String loginId,
        String name,
        Role role,
        String email,
        SignupStatus signupStatus,
        LocalDateTime createdAt
) {
    public static SignUpResDto from(SignUpResultDto result) {
        return new SignUpResDto(
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

