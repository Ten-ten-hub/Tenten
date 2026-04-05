package com.team.companyservice.infrastructure.client.dto;

import java.util.UUID;

public record UpdateUserAffiliationRequest(
    String affiliation,
    UUID affiliationId
) {
}
