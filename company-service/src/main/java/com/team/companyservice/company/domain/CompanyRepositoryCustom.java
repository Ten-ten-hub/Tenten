package com.team.companyservice.company.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.team.companyservice.company.application.search.CompanySearchCondition;

public interface CompanyRepositoryCustom {
    Page<Company> search(CompanySearchCondition condition, Pageable pageable);
}
