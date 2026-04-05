package com.team.companyservice.infrastructure.client.dto;

import java.util.UUID;

public record UpdateUserAffiliationRequest(
    AffiliationType affiliation,
    UUID affiliationId
) {
}
