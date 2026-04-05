package com.team.companyservice.company.presentation;

import com.team.companyservice.company.application.dto.response.CompanyResponse;
import com.team.companyservice.company.application.service.CompanyService;
import com.team.companyservice.company.application.dto.response.CompanyInternalResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> checkCompanyExists(@PathVariable UUID companyId) {
        companyService.get(companyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyInternalResponse> getCompany(@PathVariable UUID companyId) {
        CompanyResponse company = companyService.get(companyId);
        return ResponseEntity.ok(CompanyInternalResponse.from(company));
    }
}
