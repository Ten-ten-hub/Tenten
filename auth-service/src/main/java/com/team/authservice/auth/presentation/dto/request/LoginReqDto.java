package com.team.authservice.auth.presentation.dto.request;

public record LoginReqDto(
    String loginId,
    String password
) {
}
