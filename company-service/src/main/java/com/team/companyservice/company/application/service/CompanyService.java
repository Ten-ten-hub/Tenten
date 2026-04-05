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
import com.team.companyservice.global.common.CurrentUser;
import com.team.companyservice.global.error.CompanyErrorCode;
import com.team.companyservice.global.error.ServiceException;
import com.team.companyservice.infrastructure.client.HubClient;
import com.team.companyservice.infrastructure.client.UserClient;
import com.team.companyservice.infrastructure.client.dto.AffiliatedStatus;
import com.team.companyservice.infrastructure.client.dto.AffiliationType;
import com.team.companyservice.infrastructure.client.dto.Role;
import com.team.companyservice.infrastructure.client.dto.UpdateUserAffiliationRequest;
import com.team.companyservice.infrastructure.client.dto.UpdateUserRoleRequest;
import com.team.companyservice.infrastructure.client.dto.UserInternalResponse;
import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 업체 도메인 서비스
// 엔드포인트 접근 권한은 AOP(@RequireRole)에서 처리하고
// 여기서는 데이터 범위 검증과 비즈니스 검증만 수행한다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final HubClient hubClient;
    private final UserClient userClient;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, CurrentUser currentUser) {
        // 허브 관리자는 자기 허브에만 업체를 생성할 수 있음
        validateCreateScope(request.getHubId(), currentUser);
        validateHubExists(request.getHubId());
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

    public CompanyResponse get(UUID companyId) {
        Company company = getActiveCompany(companyId);
        return CompanyResponse.from(company);
    }

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
        validatePage(page);

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

    @Transactional
    public CompanyResponse update(UUID companyId, UpdateCompanyRequest request, CurrentUser currentUser) {
        Company company = getActiveCompany(companyId);

        // 수정 가능한 범위인지 검증
        validateUpdateScope(company, currentUser);

        validateHubExists(request.getHubId());
        validateDuplicateOnUpdate(companyId, request.getHubId(), request.getName());

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

    @Transactional
    public void delete(UUID companyId, CurrentUser currentUser) {
        Company company = getActiveCompany(companyId);

        // 삭제 가능한 범위인지 검증
        validateDeleteScope(company, currentUser);

        company.softDelete(currentUser.userId());
    }

    @Transactional
    public void assignManager(UUID companyId, UUID userId, CurrentUser currentUser) {
        Company company = getActiveCompanyForUpdate(companyId);

        // 지정 가능한 범위인지 검증
        validateAssignManagerScope(company, currentUser);

        // 이미 해당 업체에 관리자 지정이 되어 있는지 확인
        validateManagerNotAssigned(companyId);

        // 대상 사용자 조회 및 지정 가능 여부 검증
        UserInternalResponse user = getUserInfo(userId);
        validateAssignableUser(user);

        // 유저 서비스에 역할/소속 변경 요청 + 실패 시 보상 처리
        assignManagerWithCompensation(userId, companyId, user.role());
    }

    private Company getActiveCompany(UUID companyId) {
        return companyRepository.findByIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new ServiceException(CompanyErrorCode.COMPANY_NOT_FOUND));
    }

    private Company getActiveCompanyForUpdate(UUID companyId) {
        return companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId)
            .orElseThrow(() -> new ServiceException(CompanyErrorCode.COMPANY_NOT_FOUND));
    }

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

    private void validateDuplicate(UUID hubId, String name) {
        if (companyRepository.existsByHubIdAndNameAndDeletedAtIsNull(hubId, name)) {
            throw new ServiceException(CompanyErrorCode.COMPANY_DUPLICATED);
        }
    }

    private void validateDuplicateOnUpdate(UUID companyId, UUID hubId, String name) {
        if (companyRepository.existsByHubIdAndNameAndDeletedAtIsNullAndIdNot(hubId, name, companyId)) {
            throw new ServiceException(CompanyErrorCode.COMPANY_DUPLICATED);
        }
    }

    // 생성 범위 검증
    // 마스터는 전체 가능, 허브 관리자는 자기 허브만 가능
    private void validateCreateScope(UUID requestHubId, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin() && requestHubId.equals(currentUser.hubId())) {
            return;
        }

        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

    // 수정 범위 검증
    // 허브 관리자는 자기 허브의 업체만, 업체 담당자는 자기 업체만 수정 가능
    private void validateUpdateScope(Company company, CurrentUser currentUser) {
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

    // 삭제 범위 검증
    // 허브 관리자는 자기 허브 업체만 삭제 가능
    private void validateDeleteScope(Company company, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin() && company.getHubId().equals(currentUser.hubId())) {
            return;
        }

        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

    // 관리자 지정 범위 검증
    // 허브 관리자는 자기 허브 업체에만 관리자 지정 가능
    private void validateAssignManagerScope(Company company, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }

        if (currentUser.isHubAdmin() && company.getHubId().equals(currentUser.hubId())) {
            return;
        }

        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

    // 이미 업체 관리자가 존재하는지 확인
    private void validateManagerNotAssigned(UUID companyId) {
        try {
            List<UserInternalResponse> users = userClient.getUsers(
                List.of(Role.COMPANY_MANAGER),
                AffiliatedStatus.COM_AFFILIATED
            ).data();

            boolean alreadyAssigned = users != null && users.stream()
                .anyMatch(user -> companyId.equals(user.affiliationId()));

            if (alreadyAssigned) {
                throw new ServiceException(CompanyErrorCode.COMPANY_MANAGER_ALREADY_ASSIGNED);
            }
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    // 유저 서비스에서 대상 사용자 정보 조회
    private UserInternalResponse getUserInfo(UUID userId) {
        try {
            UserInternalResponse user = userClient.getUserInfo(userId).data();

            if (user == null) {
                throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
            }

            return user;
        } catch (FeignException.NotFound e) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    // 업체 관리자로 지정 가능한 사용자 조건 검증
    private void validateAssignableUser(UserInternalResponse user) {
        if (!"APPROVED".equals(user.signupStatus())) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_APPROVED);
        }

        if (!"UNAFFILIATED".equals(user.affiliatedStatus())) {
            throw new ServiceException(CompanyErrorCode.USER_ALREADY_AFFILIATED);
        }

        if ("MASTER_ADMIN".equals(user.role())
            || "HUB_ADMIN".equals(user.role())
            || "HUB_DELIVERY_MANAGER".equals(user.role())) {
            throw new ServiceException(CompanyErrorCode.USER_ROLE_NOT_ASSIGNABLE);
        }
    }

    // 유저 역할을 업체 담당자로 변경
    private void updateUserRoleToCompanyManager(UUID userId) {
        try {
            userClient.updateUserRole(userId, new UpdateUserRoleRequest(Role.COMPANY_MANAGER));
        } catch (FeignException.NotFound e) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    // 유저 소속을 해당 업체로 배정
    private void updateUserAffiliationToCompany(UUID userId, UUID companyId) {
        try {
            userClient.updateUserAffiliation(
                userId,
                new UpdateUserAffiliationRequest(AffiliationType.COMPANY, companyId)
            );
        } catch (FeignException.NotFound e) {
            throw new ServiceException(CompanyErrorCode.USER_NOT_FOUND);
        } catch (FeignException e) {
            throw new ServiceException(CompanyErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    private void assignManagerWithCompensation(UUID userId, UUID companyId, String originalRole) {
        updateUserRoleToCompanyManager(userId);

        try {
            updateUserAffiliationToCompany(userId, companyId);
        } catch (ServiceException e) {
            rollbackUserRole(userId, originalRole);
            throw e;
        }
    }

    private void rollbackUserRole(UUID userId, String originalRole) {
        try {
            userClient.updateUserRole(userId, new UpdateUserRoleRequest(Role.valueOf(originalRole)));
        } catch (Exception rollbackException) {
            log.error(
                "업체 관리자 지정 실패 후 role rollback 중 추가 실패. userId={}, originalRole={}",
                userId,
                originalRole,
                rollbackException
            );
        }
    }

    private String normalizeSortBy(String sortBy) {
        if ("updatedAt".equals(sortBy)) {
            return "updatedAt";
        }
        return "createdAt";
    }

    private Sort.Direction normalizeDirection(String direction) {
        return "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

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

    private void validatePage(int page) {
        if (page < 0) {
            throw new ServiceException(CompanyErrorCode.COMMON_INVALID_INPUT);
        }
    }
}
