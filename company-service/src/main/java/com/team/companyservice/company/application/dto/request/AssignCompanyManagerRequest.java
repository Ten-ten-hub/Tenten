package com.team.companyservice.company.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignCompanyManagerRequest(
    @NotNull UUID userId
) {
}
