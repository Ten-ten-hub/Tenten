package com.team.companyservice.company.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateCompanyActiveRequest {

    @NotNull
    private Boolean isActive;
}
