package com.team.companyservice.company.presentation;

import com.team.common.ApiResponse;
import com.team.companyservice.company.application.dto.response.CompanyInternalResponse;
import com.team.companyservice.company.application.service.CompanyService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/companies")
public class InternalCompanyController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}/exists")
    public ApiResponse<Void> checkCompanyExists(@PathVariable UUID companyId) {
        companyService.get(companyId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{companyId}")
    public ApiResponse<CompanyInternalResponse> getCompany(@PathVariable UUID companyId) {
        return ApiResponse.success(companyService.getInternalCompany(companyId));
    }
}
