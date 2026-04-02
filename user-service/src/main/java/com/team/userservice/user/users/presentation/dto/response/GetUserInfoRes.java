package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.dto.UserDataDto;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetUserInfoRes(
    UUID userId,
    Role role,
    SignupStatus signupStatus,
    String name,
    String loginId,
    String email,
    String phoneNumber,
    String slackId,
    Affiliation affiliation,
    UUID affiliationId,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static GetUserInfoRes fromUserDataDto(UserDataDto userInfo) {
        return new GetUserInfoRes(
            userInfo.userId(),
            userInfo.role(),
            userInfo.signupStatus(),
            userInfo.name(),
            userInfo.loginId(),
            userInfo.email(),
            userInfo.phoneNumber(),
            userInfo.slackId(),
            userInfo.affiliation(),
            userInfo.affiliationId(),
            userInfo.lastLoginAt(),
            userInfo.createdAt(),
            userInfo.updatedAt()
        );
    }
}
