package com.team.userservice.user.core.vo;

public record UserUpdateInfo(
    String name,
    String loginId,
    String email,
    String phoneNumber,
    String slackId
) {
}
