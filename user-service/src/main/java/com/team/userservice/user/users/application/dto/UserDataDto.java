package com.team.userservice.user.users.application.dto;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserDataDto(
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

    public static UserDataDto fromUserInfo(User user, Affiliation affiliation, UUID affiliationId) {
        return new UserDataDto(
            user.getId(),
            user.getRole(),
            user.getSignupStatus(),
            user.getName(),
            user.getLoginId(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getSlackId(),
            affiliation,
            affiliationId,
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public static UserDataDto fromMaster(User user) {
        return new UserDataDto(
            user.getId(),
            user.getRole(),
            user.getSignupStatus(),
            user.getName(),
            user.getLoginId(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getSlackId(),
            Affiliation.NONE,
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
