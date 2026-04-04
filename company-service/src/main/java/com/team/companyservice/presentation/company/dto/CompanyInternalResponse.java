package com.team.companyservice.presentation.company.dto;

import com.team.companyservice.application.company.CompanyResponse;
import java.util.UUID;

public record CompanyInternalResponse(
    UUID id,
    String name,
    String companyType,
    UUID hubId,
    String address,
    String addressDetail,
    String contactName,
    String contactSlackId,
    boolean isActive
) {
    public static CompanyInternalResponse from(CompanyResponse response) {
        return new CompanyInternalResponse(
            response.id(),
            response.name(),
            response.companyType().name(),
            response.hubId(),
            response.address(),
            response.addressDetail(),
            response.contactName(),
            response.contactSlackId(),
            response.isActive()
        );
    }
}
