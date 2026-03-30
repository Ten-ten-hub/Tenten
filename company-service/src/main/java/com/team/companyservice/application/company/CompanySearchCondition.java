package com.team.companyservice.application.company;

import java.util.UUID;

import com.team.companyservice.domain.company.CompanyType;

public record CompanySearchCondition(
        String keyword,
        CompanyType companyType,
        UUID hubId,
        Boolean isActive
) {
}