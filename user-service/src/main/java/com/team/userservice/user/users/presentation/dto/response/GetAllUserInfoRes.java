package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.core.User;
import java.util.UUID;

public record GetAllUserInfoRes(
    UUID userId,
    Role role,
    SignupStatus signupStatus,
    String name,
    String loginId,
    String email,
    String phoneNumber,
    String slackId
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
            user.getSlackId()
        );
    }
}
