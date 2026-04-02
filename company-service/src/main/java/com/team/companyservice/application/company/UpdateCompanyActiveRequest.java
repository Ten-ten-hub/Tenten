package com.team.companyservice.application.company;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateCompanyActiveRequest {

    @NotNull
    private Boolean isActive;
}
