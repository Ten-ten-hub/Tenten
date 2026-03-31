package com.team.authservice.auth.infrastructure.feign.dto;

public record UserVerifyReqDto(
    String loginId,
    String password
) {
}
