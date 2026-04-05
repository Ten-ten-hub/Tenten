package com.team.companyservice.infrastructure.client.dto;

import java.util.UUID;

public record UserInternalResponse(
    UUID userId,
    String role,
    String signupStatus,
    String name,
    String loginId,
    String email,
    String phoneNumber,
    String slackId,
    String affiliatedStatus,
    UUID affiliationId
) {
}
