package com.team.companyservice.domain.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.team.companyservice.application.company.CompanySearchCondition;

public interface CompanyRepositoryCustom {
    Page<Company> search(CompanySearchCondition condition, Pageable pageable);
}