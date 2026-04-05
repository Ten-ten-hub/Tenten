package com.team.companyservice.company.application.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record CompanyPageResponse(
        List<CompanyResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static CompanyPageResponse from(Page<CompanyResponse> page) {
        return new CompanyPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
