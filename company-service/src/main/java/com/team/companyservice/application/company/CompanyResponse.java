package com.team.companyservice.application.company;

import java.time.LocalDateTime;
import java.util.UUID;

import com.team.companyservice.domain.company.Company;
import com.team.companyservice.domain.company.CompanyType;

import lombok.Builder;

@Builder
public record CompanyResponse(
        UUID id,
        String name,
        CompanyType companyType,
        UUID hubId,
        String address,
        String addressDetail,
        String zipcode,
        String contactName,
        String contactPhone,
        String contactSlackId,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyResponse from(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .companyType(company.getCompanyType())
                .hubId(company.getHubId())
                .address(company.getAddress())
                .addressDetail(company.getAddressDetail())
                .zipcode(company.getZipcode())
                .contactName(company.getContactName())
                .contactPhone(company.getContactPhone())
                .contactSlackId(company.getContactSlackId())
                .isActive(company.isActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}