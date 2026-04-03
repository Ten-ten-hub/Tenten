package com.team.companyservice.presentation.company;

import com.team.companyservice.application.company.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/companies")
public class CompanyInternalController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}/exists")
    public ResponseEntity<Void> checkCompanyExists(
        @PathVariable UUID companyId
    ) {
        companyService.get(companyId);
        return ResponseEntity.ok().build();
    }

}
