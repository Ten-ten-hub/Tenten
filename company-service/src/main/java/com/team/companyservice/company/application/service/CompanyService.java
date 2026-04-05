package com.team.companyservice.company.application.service;

import com.team.common.page.PageSizeUtils;
import com.team.companyservice.company.application.dto.request.CreateCompanyRequest;
import com.team.companyservice.company.application.dto.request.UpdateCompanyRequest;
import com.team.companyservice.company.application.dto.response.CompanyPageResponse;
import com.team.companyservice.company.application.dto.response.CompanyResponse;
import com.team.companyservice.company.application.search.CompanySearchCondition;
import com.team.companyservice.company.domain.Company;
import com.team.companyservice.company.domain.CompanyRepository;
import com.team.companyservice.company.domain.CompanyType;
import com.team.companyservice.infrastructure.client.HubClient;
import com.team.companyservice.infrastructure.client.UserClient;
import com.team.companyservice.infrastructure.client.dto.UpdateUserAffiliationRequest;
import com.team.companyservice.infrastructure.client.dto.UpdateUserRoleRequest;
import com.team.companyservice.infrastructure.client.dto.UserCommonResponse;
import com.team.companyservice.infrastructure.client.dto.UserInternalResponse;
import java.util.List;
import com.team.companyservice.global.error.CompanyErrorCode;
import com.team.companyservice.global.common.CurrentUser;
import com.team.companyservice.global.error.ServiceException;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final HubClient hubClient;
    private final UserClient userClient;

    // 업체 생성
    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, CurrentUser currentUser) {
        // 생성 권한 검증
        validateCreatePermission(request.getHubId(), currentUser);
        // 허브 존재 여부 검증
        validateHubExists(request.getHubId());
        // 같은 허브 내 업체명 중복 검증
        validateDuplicate(request.getHubId(), request.getName());

        Company company = Company.create(
            request.getName(),
            request.getCompanyType(),
            request.getHubId(),
            request.getAddress(),
            request.getAddressDetail(),
            request.getZipcode(),
            request.getContactName(),
            request.getContactPhone(),
            request.getContactSlackId()
        );

        companyRepository.save(company);
        return CompanyResponse.from(company);
    }

    // 업체 단건 조회
    public CompanyResponse get(UUID companyId) {
        Company company = getActiveCompany(companyId);
        return CompanyResponse.from(company);
    }

    // 업체 목록/검색 조회
    public CompanyPageResponse search(
        String keyword,
        String companyType,
        UUID hubId,
        Boolean isActive,
        String sortBy,
        String direction,
        int page,
        int size
    ) {
        // 페이지 크기와 정렬 조건 정규화
        int normalizedSize = PageSizeUtils.normalize(size);
        String normalizedSortBy = normalizeSortBy(sortBy);
        Sort.Direction sortDirection = normalizeDirection(direction);

        CompanySearchCondition condition = new CompanySearchCondition(
            keyword,
            parseCompanyType(companyType),
            hubId,
            isActive
        );

        var pageable = PageRequest.of(page, normalizedSize, Sort.by(sortDirection, normalizedSortBy));
        var result = companyRepository.search(condition, pageable)
            .map(CompanyResponse::from);

        return CompanyPageResponse.from(result);
    }

    // 업체 정보 수정
    @Transactional
    public CompanyResponse update(UUID companyId, UpdateCompanyRequest request, CurrentUser currentUser) {
        Company company = getActiveCompany(companyId);

        // 수정 권한 검증
        validateUpdatePermission(company, currentUser);
        // 변경하려는 허브 존재 여부 검증
        validateHubExists(request.getHubId());
        // 수정 시 업체명 중복 검증
        validateDuplicateOnUpdate(companyId, request.getHubId(), request.getName());

        // 업체 담당자는 본인 업체만 수정 가능
        if (currentUser.isCompanyManager() && !company.getId().equals(currentUser.companyId())) {
            throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
        }

        company.update(
            request.getName(),
            request.getCompanyType(),
            request.getHubId(),
            request.getAddress(),
            request.getAddressDetail(),
            request.getZipcode(),
            request.getContactName(),
            request.getContactPhone(),
            request.getContactSlackId()
        );

        return CompanyResponse.from(company);
    }

    // 업체 삭제(논리 삭제)
    @Transactional
    public void delete(UUID companyId, CurrentUser currentUser) {
        Company company = getActiveCompany(companyId);
        validateDeletePermission(company, currentUser);
        company.softDelete(currentUser.userId());
    }

    // 업체 관리자 지정
    @Transactional
    public void assignManager(UUID companyId, UUID userId, CurrentUser currentUser) {
        // 대상 업체 조회
        Company company = getActiveCompany(companyId);

        // 업체 관리자 지정 권한 검증
        validateAssignManagerPermission(company, currentUser);

        // 이미 업체 관리자 지정 여부 검증
        validateManagerNotAssigned(companyId);

        // 유저 서비스에서 대상 사용자 조회
        UserInternalResponse user = getUserInfo(userId);

        // 업체 관리자로 지정 가능한 사용자 상태인지 검증
        validateAssignableUser(user);

        // 사용자 권한을 COMPANY_MANAGER로 변경
        updateUserRoleToCompanyManager(userId);

        // 사용자 소속을 해당 업체로 배정
        updateUserAffiliationToCompany(userId, companyId);
    }

    // 삭제되지 않은 활성 업체 조회
    private Company getActiveCompany(UUID companyId) {
        return companyRepository.findByIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new ServiceException(CompanyErrorCode.COMPANY_NOT_FOUND));
    }

    // 허브 존재 여부 검증
    private void validateHubExists(UUID hubId) {
        try {
            boolean exists = hubClient.existsHub(hubId).exists();
            if (!exists) {
                throw new ServiceException(CompanyErrorCode.HUB_NOT_FOUND);
            }
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.HUB_SERVICE_UNAVAILABLE);
        }
    }

    // 업체 생성 시 중복 검증
    private void validateDuplicate(UUID hubId, String name) {
        if (companyRepository.existsByHubIdAndNameAndDeletedAtIsNull(hubId, name)) {
            throw new ServiceException(CompanyErrorCode.COMPANY_DUPLICATED);
        }
    }

    // 업체 수정 시 본인 제외 중복 검증
    private void validateDuplicateOnUpdate(UUID companyId, UUID hubId, String name) {
        if (companyRepository.existsByHubIdAndNameAndDeletedAtIsNullAndIdNot(hubId, name, companyId)) {
            throw new ServiceException(CompanyErrorCode.COMPANY_DUPLICATED);
        }
    }

    // 업체 생성 권한 검증
    private void validateCreatePermission(UUID requestHubId, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }
        if (currentUser.isHubAdmin() && requestHubId.equals(currentUser.hubId())) {
            return;
        }
        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

    // 업체 수정 권한 검증
    private void validateUpdatePermission(Company company, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }
        if (currentUser.isHubAdmin() && company.getHubId().equals(currentUser.hubId())) {
            return;
        }
        if (currentUser.isCompanyManager() && company.getId().equals(currentUser.companyId())) {
            return;
        }
        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

    // 업체 삭제 권한 검증
    private void validateDeletePermission(Company company, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }
        if (currentUser.isHubAdmin() && company.getHubId().equals(currentUser.hubId())) {
            return;
        }
        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

    // 업체 관리자 지정 권한 검증
    private void validateAssignManagerPermission(Company company, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }
        if (currentUser.isHubAdmin() && company.getHubId().equals(currentUser.hubId())) {
            return;
        }
        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

    // 이미 해당 업체에 관리자가 지정되어 있는지 검증
    private void validateManagerNotAssigned(UUID companyId) {
        try {
            UserCommonResponse<List<UserInternalResponse>> response = userClient.getUsers(
                List.of("COMPANY_MANAGER"),
                "COM_AFFILIATED"
            );

            if (response == null || response.data() == null) {
                throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
            }

            boolean alreadyAssigned = response.data().stream()
                .anyMatch(user -> companyId.equals(user.affiliationId()));

            if (alreadyAssigned) {
                throw new ServiceException(CompanyErrorCode.COMPANY_MANAGER_ALREADY_ASSIGNED);
            }
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    // 유저 서비스에서 사용자 정보 조회
    private UserInternalResponse getUserInfo(UUID userId) {
        try {
            UserCommonResponse<UserInternalResponse> response = userClient.getUserInfo(userId);

            if (response == null || response.data() == null) {
                throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
            }

            return response.data();
        } catch (FeignException.NotFound e) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    // 업체 관리자로 지정 가능한 사용자 상태인지 검증
    private void validateAssignableUser(UserInternalResponse user) {
        // 가입 승인된 사용자만 업체 관리자로 지정 가능
        if (!"APPROVED".equals(user.signupStatus())) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_APPROVED);
        }

        // 아직 소속이 없는 사용자만 지정 가능
        if (!"UNAFFILIATED".equals(user.affiliatedStatus())) {
            throw new ServiceException(CompanyErrorCode.USER_ALREADY_AFFILIATED);
        }

        // 아래 권한은 업체 관리자로 지정 불가
        if ("MASTER_ADMIN".equals(user.role())
            || "HUB_ADMIN".equals(user.role())
            || "HUB_DELIVERY_MANAGER".equals(user.role())) {
            throw new ServiceException(CompanyErrorCode.USER_ROLE_NOT_ASSIGNABLE);
        }
    }

    // 사용자 권한을 업체 담당자로 변경
    private void updateUserRoleToCompanyManager(UUID userId) {
        try {
            userClient.updateUserRole(userId, new UpdateUserRoleRequest("COMPANY_MANAGER"));
        } catch (FeignException.NotFound e) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    // 사용자 소속을 해당 업체로 변경
    private void updateUserAffiliationToCompany(UUID userId, UUID companyId) {
        try {
            userClient.updateUserAffiliation(
                userId,
                new UpdateUserAffiliationRequest("COMPANY", companyId)
            );
        } catch (FeignException.NotFound e) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    // 정렬 필드 정규화
    private String normalizeSortBy(String sortBy) {
        if ("updatedAt".equals(sortBy)) {
            return "updatedAt";
        }
        return "createdAt";
    }

    // 정렬 방향 정규화
    private Sort.Direction normalizeDirection(String direction) {
        return "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    // 문자열 companyType을 enum으로 변환
    private CompanyType parseCompanyType(String companyType) {
        if (companyType == null || companyType.isBlank()) {
            return null;
        }

        try {
            return CompanyType.valueOf(companyType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException(CompanyErrorCode.COMMON_INVALID_INPUT);
        }
    }

}
