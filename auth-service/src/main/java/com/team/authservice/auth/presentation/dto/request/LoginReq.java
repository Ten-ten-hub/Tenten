package com.team.authservice.auth.presentation.dto.request;

public record LoginReq(
    String loginId,
    String password
) {
}
