package com.team.companyservice.company.presentation;

import com.team.companyservice.company.application.dto.request.AssignCompanyManagerRequest;
import com.team.companyservice.company.application.dto.request.CreateCompanyRequest;
import com.team.companyservice.company.application.dto.request.UpdateCompanyRequest;
import com.team.companyservice.company.application.dto.response.CompanyPageResponse;
import com.team.companyservice.company.application.dto.response.CompanyResponse;
import com.team.companyservice.company.application.service.CompanyService;
import com.team.companyservice.global.auth.RequireRole;
import com.team.companyservice.global.common.ApiResponse;
import com.team.companyservice.global.common.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 외부 호출용 업체 API 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies")
public class ExternalCompanyController {

    private final CompanyService companyService;

    // 업체 생성은 마스터 관리자 / 허브 관리자 / 업체 관리자만 가능
    @RequireRole({"MASTER_ADMIN",
        "HUB_ADMIN",
        "COMPANY_MANAGER"})
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(
        @Valid @RequestBody CreateCompanyRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(companyService.create(request, currentUser)));
    }

    // 업체 단건 조회는 모든 업무 역할 사용자에게 허용
    @RequireRole({"MASTER_ADMIN",
        "HUB_ADMIN",
        "COMPANY_MANAGER"
        ,
        "HUB_DELIVERY_MANAGER",
        "COM_DELIVERY_MANAGER"
    })
    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> get(
        @PathVariable UUID companyId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.get(companyId)));
    }

    // 업체 목록 조회/검색은 모든 업무 역할 사용자에게 허용
    @RequireRole({
        "MASTER_ADMIN",
        "HUB_ADMIN",
        "COMPANY_MANAGER",
        "HUB_DELIVERY_MANAGER",
        "COM_DELIVERY_MANAGER"
    })
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

    // 업체 수정은 마스터 관리자 / 허브 관리자 / 업체 담당자만 가능
    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN", "COMPANY_MANAGER"})
    @PutMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> update(
        @PathVariable UUID companyId,
        @Valid @RequestBody UpdateCompanyRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.update(companyId, request, currentUser)));
    }

    // 업체 삭제는 현재 정책상 마스터 관리자 / 허브 관리자 허용
    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN"})
    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable UUID companyId,
        CurrentUser currentUser
    ) {
        companyService.delete(companyId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 업체 관리자 지정은 마스터 관리자 / 허브 관리자 / 업체 관리자 만 가능
    @RequireRole({"MASTER_ADMIN",
        "HUB_ADMIN",
        "COMPANY_MANAGER"})
    @PatchMapping("/{companyId}/manager")
    public ResponseEntity<ApiResponse<Void>> assignManager(
        @PathVariable UUID companyId,
        @Valid @RequestBody AssignCompanyManagerRequest request,
        CurrentUser currentUser
    ) {
        companyService.assignManager(companyId, request.userId(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
