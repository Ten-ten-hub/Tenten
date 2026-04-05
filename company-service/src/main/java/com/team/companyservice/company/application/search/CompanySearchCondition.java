package com.team.companyservice.company.application.search;

import java.util.UUID;

import com.team.companyservice.company.domain.CompanyType;

public record CompanySearchCondition(
        String keyword,
        CompanyType companyType,
        UUID hubId,
        Boolean isActive
) {
}
