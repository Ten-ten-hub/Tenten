package com.team.authservice.auth.infrastructure.feign.dto;

public record UserVerifyReq(
    String loginId,
    String password
) {
}
