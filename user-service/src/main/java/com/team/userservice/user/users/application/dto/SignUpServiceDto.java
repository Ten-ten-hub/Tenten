package com.team.userservice.user.users.application.dto;

public record SignUpServiceDto(
    String name,
    String loginId,
    String password,
    String slackId,
    String email,
    String phoneNumber
) {
}
