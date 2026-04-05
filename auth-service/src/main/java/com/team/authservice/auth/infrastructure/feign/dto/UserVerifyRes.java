package com.team.authservice.auth.infrastructure.feign.dto;

import com.team.authservice.core.enums.Role;
import java.util.UUID;

public record UserVerifyRes(
    UUID userId,
    Role role,
    UUID hubId,
    UUID companyId
) {
}
