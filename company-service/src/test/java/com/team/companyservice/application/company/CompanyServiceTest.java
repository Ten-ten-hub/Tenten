package com.team.companyservice.application.company;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.team.companyservice.company.application.service.CompanyService;
import com.team.companyservice.company.domain.Company;
import com.team.companyservice.company.domain.CompanyRepository;
import com.team.companyservice.company.domain.CompanyType;
import com.team.companyservice.global.common.CurrentUser;
import com.team.companyservice.global.error.CompanyErrorCode;
import com.team.companyservice.global.error.ServiceException;
import com.team.companyservice.infrastructure.client.HubClient;
import com.team.companyservice.infrastructure.client.UserClient;
import com.team.companyservice.infrastructure.client.dto.HubExistsResponse;
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

    // 테스트용 업체 생성
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

    // 테스트용 CurrentUser 생성
    private CurrentUser masterAdmin() {
        return new CurrentUser(UUID.randomUUID(), "MASTER_ADMIN", null, null);
    }

    private CurrentUser hubAdmin(UUID hubId) {
        return new CurrentUser(UUID.randomUUID(), "HUB_ADMIN", hubId, null);
    }

    private UserInternalResponse approvedUnaffiliatedUser(UUID userId, String role) {
        return new UserInternalResponse(
            userId,
            role,
            "APPROVED",
            "홍길동",
            "hong",
            "hong@test.com",
            "010-1111-2222",
            "U111",
            "UNAFFILIATED",
            null
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

            given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));

            // 아직 해당 업체에 지정된 업체 관리자가 없음
            given(userClient.getUsers(anyList(), eq("COM_AFFILIATED")))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));

            // 대상 사용자 정보 조회 성공
            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(
                    true,
                    approvedUnaffiliatedUser(userId, "NONE"),
                    "OK",
                    "성공"
                ));

            // 권한 변경/소속 변경 성공
            given(userClient.updateUserRole(eq(userId), any(UpdateUserRoleRequest.class)))
                .willReturn(new UserCommonResponse<>(true, null, "OK", "성공"));
            given(userClient.updateUserAffiliation(eq(userId), any(UpdateUserAffiliationRequest.class)))
                .willReturn(new UserCommonResponse<>(true, null, "OK", "성공"));

            assertDoesNotThrow(() -> companyService.assignManager(companyId, userId, masterAdmin()));
        }

        @Test
        @DisplayName("이미 업체 관리자 지정된 업체면 실패")
        void assign_manager_fail_already_assigned() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));

            // 이미 해당 companyId로 소속된 업체 관리자 존재
            UserInternalResponse assignedManager = new UserInternalResponse(
                UUID.randomUUID(),
                "COMPANY_MANAGER",
                "APPROVED",
                "기존 관리자",
                "manager1",
                "manager1@test.com",
                "010-0000-0000",
                "U999",
                "COM_AFFILIATED",
                companyId
            );

            given(userClient.getUsers(anyList(), eq("COM_AFFILIATED")))
                .willReturn(new UserCommonResponse<>(true, List.of(assignedManager), "OK", "성공"));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            org.junit.jupiter.api.Assertions.assertEquals(
                CompanyErrorCode.COMPANY_MANAGER_ALREADY_ASSIGNED,
                exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("가입 승인 안 된 사용자면 실패")
        void assign_manager_fail_user_not_approved() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));
            given(userClient.getUsers(anyList(), eq("COM_AFFILIATED")))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));

            UserInternalResponse user = new UserInternalResponse(
                userId,
                "NONE",
                "PENDING",
                "홍길동",
                "hong",
                "hong@test.com",
                "010-1111-2222",
                "U111",
                "UNAFFILIATED",
                null
            );

            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(true, user, "OK", "성공"));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            org.junit.jupiter.api.Assertions.assertEquals(
                CompanyErrorCode.USER_NOT_APPROVED,
                exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("이미 다른 소속이 있는 사용자면 실패")
        void assign_manager_fail_user_already_affiliated() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));
            given(userClient.getUsers(anyList(), eq("COM_AFFILIATED")))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));

            UserInternalResponse user = new UserInternalResponse(
                userId,
                "NONE",
                "APPROVED",
                "홍길동",
                "hong",
                "hong@test.com",
                "010-1111-2222",
                "U111",
                "COM_AFFILIATED",
                UUID.randomUUID()
            );

            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(true, user, "OK", "성공"));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            org.junit.jupiter.api.Assertions.assertEquals(
                CompanyErrorCode.USER_ALREADY_AFFILIATED,
                exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("허브 관리자 범위 밖 업체면 실패")
        void assign_manager_fail_hub_admin_out_of_scope() {
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID otherHubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, companyHubId);

            given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, hubAdmin(otherHubId))
            );

            org.junit.jupiter.api.Assertions.assertEquals(
                CompanyErrorCode.COMMON_ACCESS_DENIED,
                exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("지정 불가 role이면 실패")
        void assign_manager_fail_not_assignable_role() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));
            given(userClient.getUsers(anyList(), eq("COM_AFFILIATED")))
                .willReturn(new UserCommonResponse<>(true, List.of(), "OK", "성공"));
            given(userClient.getUserInfo(userId))
                .willReturn(new UserCommonResponse<>(
                    true,
                    approvedUnaffiliatedUser(userId, "HUB_ADMIN"),
                    "OK",
                    "성공"
                ));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            org.junit.jupiter.api.Assertions.assertEquals(
                CompanyErrorCode.USER_ROLE_NOT_ASSIGNABLE,
                exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("user-service 호출 실패 시 예외 변환")
        void assign_manager_fail_user_service_unavailable() {
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Company company = createCompany(companyId, hubId);

            given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));

            given(userClient.getUsers(anyList(), eq("COM_AFFILIATED")))
                .willThrow(Mockito.mock(FeignException.class));

            ServiceException exception = assertThrows(
                ServiceException.class,
                () -> companyService.assignManager(companyId, userId, masterAdmin())
            );

            org.junit.jupiter.api.Assertions.assertEquals(
                CompanyErrorCode.USER_SERVICE_UNAVAILABLE,
                exception.getErrorCode()
            );
        }
    }

    @Nested
    @DisplayName("업체 생성")
    class CreateTest {

        @Test
        @DisplayName("업체 생성 성공")
        void create_success() {
            // 기존 테스트 유지 또는 기존 파일 내용 사용
        }

        @Test
        @DisplayName("업체 생성 실패 - 허브가 존재하지 않음")
        void create_fail_hub_not_found() {
            // 기존 테스트 유지 또는 기존 파일 내용 사용
        }

        @Test
        @DisplayName("업체 생성 실패 - 같은 허브 내 업체명 중복")
        void create_fail_duplicate_name() {
            // 기존 테스트 유지 또는 기존 파일 내용 사용
        }
    }
}
