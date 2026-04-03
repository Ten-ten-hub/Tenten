package com.team.userservice.user.users.presentation.dto.request;

import com.team.userservice.user.users.application.dto.UpdateUserServiceDto;

public record UpdateUserReq(
    String name,
    String loginId,
    String email,
    String phoneNumber,
    String slackId
) {
    public UpdateUserServiceDto toServiceDto() {
        return new UpdateUserServiceDto(name, loginId, email, phoneNumber, slackId);
    }

}
