package com.team.userservice.user.users.presentation.dto.request;

import com.team.userservice.user.core.enums.Affiliation;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateUserAffiliationReq(
    @NotNull Affiliation affiliation,
    @NotNull UUID affiliationId
) {
}
