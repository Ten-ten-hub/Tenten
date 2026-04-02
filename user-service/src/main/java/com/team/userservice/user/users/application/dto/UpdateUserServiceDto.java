package com.team.userservice.user.users.application.dto;

import com.team.userservice.user.core.vo.UserUpdateInfo;

public record UpdateUserServiceDto(
    String name,
    String loginId,
    String email,
    String phoneNumber,
    String slackId
) {
    public UserUpdateInfo toEntityDto() {
        return new UserUpdateInfo(name, loginId, email, phoneNumber, slackId);
    }
}
