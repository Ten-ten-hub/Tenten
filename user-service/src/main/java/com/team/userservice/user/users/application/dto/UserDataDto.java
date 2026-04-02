package com.team.userservice.user.users.application.dto;

import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
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
    AffiliatedStatus affiliatedStatus,
    UUID affiliationId,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static UserDataDto fromUserInfo(User user, UUID affiliationId) {
        return new UserDataDto(
            user.getId(),
            user.getRole(),
            user.getSignupStatus(),
            user.getName(),
            user.getLoginId(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getSlackId(),
            user.getAffiliatedStatus(),
            affiliationId,
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public static UserDataDto fromUserInfo(User user) {
        return new UserDataDto(
            user.getId(),
            user.getRole(),
            user.getSignupStatus(),
            user.getName(),
            user.getLoginId(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getSlackId(),
            user.getAffiliatedStatus(),
            null,
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
