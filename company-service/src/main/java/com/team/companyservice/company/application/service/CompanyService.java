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

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, CurrentUser currentUser) {
        validateCreatePermission(request.getHubId(), currentUser);
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
        validateUpdatePermission(company, currentUser);
        validateHubExists(request.getHubId());
        validateDuplicateOnUpdate(companyId, request.getHubId(), request.getName());

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

    @Transactional
    public void delete(UUID companyId, CurrentUser currentUser) {
        Company company = getActiveCompany(companyId);
        validateDeletePermission(company, currentUser);
        company.softDelete(currentUser.userId());
    }

    private Company getActiveCompany(UUID companyId) {
        return companyRepository.findByIdAndDeletedAtIsNull(companyId)
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

    private void validateCreatePermission(UUID requestHubId, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }
        if (currentUser.isHubAdmin() && requestHubId.equals(currentUser.hubId())) {
            return;
        }
        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
    }

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

    private void validateDeletePermission(Company company, CurrentUser currentUser) {
        if (currentUser.isMasterAdmin()) {
            return;
        }
        if (currentUser.isHubAdmin() && company.getHubId().equals(currentUser.hubId())) {
            return;
        }
        throw new ServiceException(CompanyErrorCode.COMMON_ACCESS_DENIED);
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
}
