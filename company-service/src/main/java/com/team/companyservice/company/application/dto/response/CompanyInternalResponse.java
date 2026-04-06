package com.team.companyservice.company.application.dto.response;

import com.team.companyservice.company.domain.Company;
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
    boolean active
) {
    public static CompanyInternalResponse from(Company company) {
        return new CompanyInternalResponse(
            company.getId(),
            company.getName(),
            company.getCompanyType().name(),
            company.getHubId(),
            company.getAddress(),
            company.getAddressDetail(),
            company.getContactName(),
            company.getContactSlackId(),
            company.isActive()
        );
    }
}
