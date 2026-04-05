package com.team.userservice.user.users.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.user.companies.domain.CompanyRepository;
import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.hubs.domain.HubRepository;
import com.team.userservice.user.users.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final HubRepository hubRepository = Mockito.mock(HubRepository.class);
    private final CompanyRepository companyRepository = Mockito.mock(CompanyRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

    private final UserServiceImpl userService = new UserServiceImpl(
        userRepository,
        hubRepository,
        companyRepository,
        passwordEncoder
    );

    // 테스트용 User mock 생성
    private User user(Role role, AffiliatedStatus affiliatedStatus) {
        User user = Mockito.mock(User.class);
        given(user.getRole()).willReturn(role);
        given(user.getAffiliatedStatus()).willReturn(affiliatedStatus);
        return user;
    }

    @Test
    @DisplayName("role 변경 성공")
    void update_user_role_success() {
        UUID userId = UUID.randomUUID();
        User user = user(Role.NONE, AffiliatedStatus.UNAFFILIATED);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.updateUserRole(userId, Role.COMPANY_MANAGER));
    }

    @Test
    @DisplayName("존재하지 않는 user면 role 변경 실패")
    void update_user_role_fail_user_not_found() {
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        UserException exception = assertThrows(
            UserException.class,
            () -> userService.updateUserRole(userId, Role.COMPANY_MANAGER)
        );

        org.junit.jupiter.api.Assertions.assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("affiliation 변경 성공 - unaffiliated 사용자를 업체에 배정")
    void update_user_affiliation_success_company() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        User user = user(Role.COMPANY_MANAGER, AffiliatedStatus.UNAFFILIATED);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.updateUserAffiliation(userId, Affiliation.COMPANY, companyId));
    }

    @Test
    @DisplayName("role-affiliation conflict - HUB_ADMIN은 COMPANY 소속 배정 실패")
    void update_user_affiliation_fail_role_affiliation_conflict() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        User user = user(Role.HUB_ADMIN, AffiliatedStatus.UNAFFILIATED);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        UserException exception = assertThrows(
            UserException.class,
            () -> userService.updateUserAffiliation(userId, Affiliation.COMPANY, companyId)
        );

        org.junit.jupiter.api.Assertions.assertEquals(UserErrorCode.ROLE_AFFILIATION_CONFLICT, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 업체 소속인 경우 기존 소속을 변경")
    void update_user_affiliation_success_update_existing_company() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        User user = user(Role.COMPANY_MANAGER, AffiliatedStatus.COM_AFFILIATED);
        CompanyUser companyUser = Mockito.mock(CompanyUser.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(companyRepository.findByUser(user)).willReturn(companyUser);

        assertDoesNotThrow(() -> userService.updateUserAffiliation(userId, Affiliation.COMPANY, companyId));
    }

    @Test
    @DisplayName("이미 허브 소속인 경우 기존 소속을 변경")
    void update_user_affiliation_success_update_existing_hub() {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        User user = user(Role.HUB_ADMIN, AffiliatedStatus.HUB_AFFILIATED);
        HubUser hubUser = Mockito.mock(HubUser.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(hubRepository.findByUser(user)).willReturn(hubUser);

        assertDoesNotThrow(() -> userService.updateUserAffiliation(userId, Affiliation.HUB, hubId));
    }

    @Test
    @DisplayName("존재하지 않는 user면 affiliation 변경 실패")
    void update_user_affiliation_fail_user_not_found() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        UserException exception = assertThrows(
            UserException.class,
            () -> userService.updateUserAffiliation(userId, Affiliation.COMPANY, companyId)
        );

        org.junit.jupiter.api.Assertions.assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
