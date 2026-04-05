package com.team.userservice.user.users.presentation.dto.response;

import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.core.enums.SignupStatus;
import com.team.userservice.user.users.application.dto.UserDataDto;
import java.util.UUID;

/**
 * 내부 다건 조회 전용 사용자 요약 응답 DTO
 */
public record InternalUserSummaryRes(
    UUID userId,
    Role role,
    SignupStatus signupStatus,
    AffiliatedStatus affiliatedStatus,
    UUID affiliationId
) {
    public static InternalUserSummaryRes from(UserDataDto userInfo) {
        return new InternalUserSummaryRes(
            userInfo.userId(),
            userInfo.role(),
            userInfo.signupStatus(),
            userInfo.affiliatedStatus(),
            userInfo.affiliationId()
        );
    }
}
