package com.team.companyservice.application.company;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team.companyservice.company.application.dto.request.CreateCompanyRequest;
import com.team.companyservice.company.application.service.CompanyService;
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
import com.team.companyservice.infrastructure.client.dto.HubExistsResponse;
import com.team.companyservice.infrastructure.client.dto.Role;
import com.team.companyservice.infrastructure.client.dto.UpdateUserAffiliationRequest;
import com.team.companyservice.infrastructure.client.dto.UpdateUserRoleRequest;
import com.team.companyservice.infrastructure.client.dto.UserCommonResponse;
import com.team.companyservice.infrastructure.client.dto.UserInternalResponse;
import feign.FeignException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompanyServiceTest {

    private final CompanyRepository companyRepository = Mockito.mock(CompanyRepository.class);
    private final HubClient hubClient = Mockito.mock(HubClient.class);
    private final UserClient userClient = Mockito.mock(UserClient.class);

    private final CompanyService companyService = new CompanyService(
        companyRepository,
        hubClient,
        userClient
    );

    private Company createCompany(UUID companyId, UUID hubId) {
        return Company.builder()
            .id(companyId)
            .name("테스트 업체")
            .companyType(CompanyType.PRODUCER)
            .hubId(hubId)
            .address("서울시 강남구")
            .addressDetail("101호")
            .zipcode("12345")
            .contactName("담당자")
            .contactPhone("010-1111-2222")
            .contactSlackId("U123")
            .isActive(true)
            .build();
    }

    private CurrentUser masterAdmin() {
        return new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);
    }

    private CurrentUser hubAdmin(UUID hubId) {
        return new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);
    }

    private UserInternalResponse user(
        UUID userId,
        String role,
        String signupStatus,
        String affiliatedStatus,
        UUID affiliationId
    ) {
        return new UserInternalResponse(
            userId,
            role,
            signupStatus,
            affiliatedStatus,
            affiliationId
        );
    }

    @Nested
    @DisplayName("업체 관리자 지정")
    class AssignManagerTest {

        @Test
        @DisplayName("업체 관리자 지정 성공")
        void assign_manager_success() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId))
                .willReturn(Optional.of(company));

            given(userClient.getUsers(eq(List.of(Role.COMPANY_MANAGER)), eq(AffiliatedStatus.COM_AFFILIATED)))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));

            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(
                    true,
                    user(userId, "NONE", "APPROVED", "UNAFFILIATED", null),
                    "OK",
                    "성공"
                ));

            given(userClient.updateUserRole(eq(userId), any(UpdateUserRoleRequest.class)))
                .willReturn(new UserCommonResponse<>(true, null, "OK", "성공"));

            given(userClient.updateUserAffiliation(eq(userId), any(UpdateUserAffiliationRequest.class)))
                .willReturn(new UserCommonResponse<>(true, null, "OK", "성공"));

            assertDoesNotThrow(() -> companyService.assignManager(companyId, userId, masterAdmin()));

            ArgumentCaptor<UpdateUserRoleRequest> roleCaptor =
                ArgumentCaptor.forClass(UpdateUserRoleRequest.class);
            ArgumentCaptor<UpdateUserAffiliationRequest> affiliationCaptor =
                ArgumentCaptor.forClass(UpdateUserAffiliationRequest.class);

            verify(userClient).getUsers(eq(List.of(Role.COMPANY_MANAGER)), eq(AffiliatedStatus.COM_AFFILIATED));
            verify(userClient).updateUserRole(eq(userId), roleCaptor.capture());
            verify(userClient).updateUserAffiliation(eq(userId), affiliationCaptor.capture());

            assertEquals(Role.COMPANY_MANAGER, roleCaptor.getValue().role());
            assertEquals(AffiliationType.COMPANY, affiliationCaptor.getValue().affiliation());
            assertEquals(companyId, affiliationCaptor.getValue().affiliationId());
        }

        @Test
        @DisplayName("이미 업체 관리자 지정된 업체면 실패")
        void assign_manager_fail_already_assigned() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId))
                .willReturn(Optional.of(company));

            UserInternalResponse assignedManager = user(
                UUID.randomUUID(),
                "COMPANY_MANAGER",
                "APPROVED",
                "COM_AFFILIATED",
                companyId
            );

            given(userClient.getUsers(eq(List.of(Role.COMPANY_MANAGER)), eq(AffiliatedStatus.COM_AFFILIATED)))
                .willReturn(new UserCommonResponse<>(true, List.of(assignedManager), "OK", "성공"));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            assertEquals(CompanyErrorCode.COMPANY_MANAGER_ALREADY_ASSIGNED, exception.getErrorCode());
        }

        @Test
        @DisplayName("가입 승인 안 된 사용자면 실패")
        void assign_manager_fail_user_not_approved() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId))
                .willReturn(Optional.of(company));
            given(userClient.getUsers(eq(List.of(Role.COMPANY_MANAGER)), eq(AffiliatedStatus.COM_AFFILIATED)))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));

            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(
                    true,
                    user(userId, "NONE", "PENDING", "UNAFFILIATED", null),
                    "OK",
                    "성공"
                ));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            assertEquals(CompanyErrorCode.USER_NOT_APPROVED, exception.getErrorCode());
        }

        @Test
        @DisplayName("이미 다른 소속이 있는 사용자면 실패")
        void assign_manager_fail_user_already_affiliated() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId))
                .willReturn(Optional.of(company));
            given(userClient.getUsers(eq(List.of(Role.COMPANY_MANAGER)), eq(AffiliatedStatus.COM_AFFILIATED)))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));

            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(
                    true,
                    user(userId, "NONE", "APPROVED", "COM_AFFILIATED", UUID.randomUUID()),
                    "OK",
                    "성공"
                ));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            assertEquals(CompanyErrorCode.USER_ALREADY_AFFILIATED, exception.getErrorCode());
        }

        @Test
        @DisplayName("허브 관리자 범위 밖 업체면 실패")
        void assign_manager_fail_hub_admin_out_of_scope() {
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID otherHubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, companyHubId);

            given(companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId))
                .willReturn(Optional.of(company));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, hubAdmin(otherHubId))
            );

            assertEquals(CompanyErrorCode.COMMON_ACCESS_DENIED, exception.getErrorCode());
        }

        @Test
        @DisplayName("지정 불가 권한이면 실패")
        void assign_manager_fail_not_assignable_role() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId))
                .willReturn(Optional.of(company));
            given(userClient.getUsers(eq(List.of(Role.COMPANY_MANAGER)), eq(AffiliatedStatus.COM_AFFILIATED)))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));
            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(
                    true,
                    user(userId, "HUB_ADMIN", "APPROVED", "UNAFFILIATED", null),
                    "OK",
                    "성공"
                ));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            assertEquals(CompanyErrorCode.USER_ROLE_NOT_ASSIGNABLE, exception.getErrorCode());
        }

        @Test
        @DisplayName("user-service 호출 실패 시 예외 변환")
        void assign_manager_fail_user_service_unavailable() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNullForUpdate(companyId))
                .willReturn(Optional.of(company));

            given(userClient.getUsers(eq(List.of(Role.COMPANY_MANAGER)), eq(AffiliatedStatus.COM_AFFILIATED)))
                .willThrow(Mockito.mock(FeignException.class));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            assertEquals(CompanyErrorCode.USER_SERVICE_UNAVAILABLE, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("업체 생성")
    class CreateTest {

        @Test
        @DisplayName("업체 생성 성공")
        void create_success() {
            UUID hubId = UUID.randomUUID();

            CreateCompanyRequest request = Mockito.mock(CreateCompanyRequest.class);
            given(request.getHubId()).willReturn(hubId);
            given(request.getName()).willReturn("새 업체");
            given(request.getCompanyType()).willReturn(CompanyType.PRODUCER);
            given(request.getAddress()).willReturn("서울시 송파구");
            given(request.getAddressDetail()).willReturn("101호");
            given(request.getZipcode()).willReturn("12345");
            given(request.getContactName()).willReturn("담당자");
            given(request.getContactPhone()).willReturn("010-1111-2222");
            given(request.getContactSlackId()).willReturn("U123");

            given(hubClient.existsHub(hubId))
                .willReturn(new HubExistsResponse(true, new HubExistsResponse.HubExistsData(hubId, true), "OK", "성공"));
            given(companyRepository.existsByHubIdAndNameAndDeletedAtIsNull(hubId, "새 업체"))
                .willReturn(false);

            assertDoesNotThrow(() -> companyService.create(request, masterAdmin()));
        }

        @Test
        @DisplayName("업체 생성 실패 - 허브가 존재하지 않음")
        void create_fail_hub_not_found() {
            UUID hubId = UUID.randomUUID();

            CreateCompanyRequest request = Mockito.mock(CreateCompanyRequest.class);
            given(request.getHubId()).willReturn(hubId);
            given(request.getName()).willReturn("새 업체");

            given(hubClient.existsHub(hubId))
                .willReturn(new HubExistsResponse(true, new HubExistsResponse.HubExistsData(hubId, false), "OK", "성공"));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.create(request, masterAdmin())
            );

            assertEquals(CompanyErrorCode.HUB_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("업체 생성 실패 - 같은 허브 내 업체명 중복")
        void create_fail_duplicate_name() {
            UUID hubId = UUID.randomUUID();

            CreateCompanyRequest request = Mockito.mock(CreateCompanyRequest.class);
            given(request.getHubId()).willReturn(hubId);
            given(request.getName()).willReturn("중복 업체");
            given(request.getCompanyType()).willReturn(CompanyType.PRODUCER);
            given(request.getAddress()).willReturn("서울시 강남구");

            given(hubClient.existsHub(hubId))
                .willReturn(new HubExistsResponse(true, new HubExistsResponse.HubExistsData(hubId, true), "OK", "성공"));
            given(companyRepository.existsByHubIdAndNameAndDeletedAtIsNull(hubId, "중복 업체"))
                .willReturn(true);

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.create(request, masterAdmin())
            );

            assertEquals(CompanyErrorCode.COMPANY_DUPLICATED, exception.getErrorCode());
        }
    }
}
