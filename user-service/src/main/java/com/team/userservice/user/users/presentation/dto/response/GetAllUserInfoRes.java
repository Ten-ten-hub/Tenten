package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.core.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetAllUserInfoRes(
    UUID userId,
    Role role,
    SignupStatus signupStatus,
    String name,
    String loginId,
    String email,
    String phoneNumber,
    String slackId,
    AffiliatedStatus affiliatedStatus,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static GetAllUserInfoRes from(User user) {
        return new GetAllUserInfoRes(
            user.getId(),
            user.getRole(),
            user.getSignupStatus(),
            user.getName(),
            user.getLoginId(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getSlackId(),
            user.getAffiliatedStatus(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
