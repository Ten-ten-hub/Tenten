package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.dto.UserDataDto;
import java.util.UUID;

/**
 * 내부 단건 조회 전용 사용자 응답 DTO
 */
public record InternalUserInfoRes(
    UUID userId,
    Role role,
    SignupStatus signupStatus,
    AffiliatedStatus affiliatedStatus,
    UUID affiliationId
) {
    public static InternalUserInfoRes from(UserDataDto userInfo) {
        return new InternalUserInfoRes(
            userInfo.userId(),
            userInfo.role(),
            userInfo.signupStatus(),
            userInfo.affiliatedStatus(),
            userInfo.affiliationId()
        );
    }
}
