package com.team.companyservice.presentation.company;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.team.companyservice.application.company.CompanyPageResponse;
import com.team.companyservice.application.company.CompanyResponse;
import com.team.companyservice.application.company.CompanyService;
import com.team.companyservice.application.company.CreateCompanyRequest;
import com.team.companyservice.application.company.UpdateCompanyRequest;
import com.team.companyservice.presentation.common.ApiResponse;
import com.team.companyservice.presentation.common.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(
            @Valid @RequestBody CreateCompanyRequest request,
            CurrentUser currentUser
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(companyService.create(request, currentUser)));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> get(
            @PathVariable UUID companyId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.get(companyId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CompanyPageResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String companyType,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                companyService.search(keyword, companyType, hubId, isActive, sortBy, direction, page, size)
        ));
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> update(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanyRequest request,
            CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.update(companyId, request, currentUser)));
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID companyId,
            CurrentUser currentUser
    ) {
        companyService.delete(companyId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}