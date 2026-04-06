package com.team.userservice.user.users.application;

import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.support.UserFixture;
import com.team.userservice.user.companies.application.CompanyService;
import com.team.userservice.user.core.CompanyUser;
import com.team.userservice.user.core.HubUser;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.hubs.application.HubService;
import com.team.userservice.user.users.application.dto.LoginServiceDto;
import com.team.userservice.user.users.application.dto.SignUpResultDto;
import com.team.userservice.user.users.application.dto.UpdateUserServiceDto;
import com.team.userservice.user.users.application.dto.UserDataDto;
import com.team.userservice.user.users.domain.UserRepository;
import com.team.userservice.user.users.infrastructure.feignClient.CompanyInternalClient;
import com.team.userservice.user.users.infrastructure.feignClient.HubInternalClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HubService hubService;

    @Mock
    private CompanyService companyService;

    @Mock
    private HubInternalClient hubInternalClient;

    @Mock
    private CompanyInternalClient companyInternalClient;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = UserFixture.createUser();
    }

    // ---------------------------------------------------------------
    // signUp()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        given(userRepository.existsByLoginId(UserFixture.TEST_LOGIN_ID)).willReturn(false);
        given(userRepository.existsByEmail(UserFixture.TEST_EMAIL)).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);

        SignUpResultDto result = userService.signUp(UserFixture.createSignUpServiceDto());

        assertThat(result.loginId()).isEqualTo(UserFixture.TEST_LOGIN_ID);
        assertThat(result.name()).isEqualTo(UserFixture.TEST_NAME);
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 로그인 아이디")
    void signUp_duplicateLoginId() {
        given(userRepository.existsByLoginId(UserFixture.TEST_LOGIN_ID)).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(UserFixture.createSignUpServiceDto()))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.DUPLICATE_LOGIN_ID);

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void signUp_duplicateEmail() {
        given(userRepository.existsByLoginId(UserFixture.TEST_LOGIN_ID)).willReturn(false);
        given(userRepository.existsByEmail(UserFixture.TEST_EMAIL)).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(UserFixture.createSignUpServiceDto()))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("회원가입 실패 - DB 유니크 제약 위반")
    void signUp_dataIntegrityViolation() {
        given(userRepository.existsByLoginId(UserFixture.TEST_LOGIN_ID)).willReturn(false);
        given(userRepository.existsByEmail(UserFixture.TEST_EMAIL)).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> userService.signUp(UserFixture.createSignUpServiceDto()))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.DUPLICATE_USER_INFO);
    }

    // ---------------------------------------------------------------
    // register()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("가입 승인 성공")
    void register_success() {
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        userService.register(UserFixture.TEST_USER_ID);

        assertThat(user.getSignupStatus()).isNotNull();
    }

    @Test
    @DisplayName("가입 승인 실패 - 존재하지 않는 유저")
    void register_userNotFound() {
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register(UserFixture.TEST_USER_ID))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("가입 승인 실패 - 이미 승인된 유저")
    void register_alreadyApproved() {
        user.register(); // PENDING → APPROVED

        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.register(UserFixture.TEST_USER_ID))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.ALREADY_REGISTERED_USER);
    }

    // ---------------------------------------------------------------
    // loginService()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("로그인 성공")
    void loginService_success() {
        given(userRepository.findByLoginId(UserFixture.TEST_LOGIN_ID)).willReturn(user);
        given(passwordEncoder.matches(UserFixture.TEST_PASSWORD, "encodedPassword")).willReturn(true);

        LoginServiceDto result = userService.loginService(UserFixture.TEST_LOGIN_ID, UserFixture.TEST_PASSWORD);

        assertThat(result.uuid()).isEqualTo(UserFixture.TEST_USER_ID);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void loginService_invalidPassword() {
        given(userRepository.findByLoginId(UserFixture.TEST_LOGIN_ID)).willReturn(user);
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> userService.loginService(UserFixture.TEST_LOGIN_ID, "wrongPassword"))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
    }

    // ---------------------------------------------------------------
    // updateUserRole()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 권한 수정 성공")
    void updateUserRole_success() {
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        userService.updateUserRole(UserFixture.TEST_USER_ID, Role.HUB_ADMIN);

        then(userRepository).should().updateUserRole(user, Role.HUB_ADMIN);
    }

    @Test
    @DisplayName("사용자 권한 수정 실패 - 존재하지 않는 유저")
    void updateUserRole_userNotFound() {
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(UserFixture.TEST_USER_ID, Role.HUB_ADMIN))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // userUpdate()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 정보 수정 성공")
    void userUpdate_success() {
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));
        UpdateUserServiceDto dto = new UpdateUserServiceDto("새이름", null, null, null, null);

        userService.userUpdate(UserFixture.TEST_USER_ID, dto);

        assertThat(user.getName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 존재하지 않는 유저")
    void userUpdate_userNotFound() {
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());
        UpdateUserServiceDto dto = new UpdateUserServiceDto("새이름", null, null, null, null);

        assertThatThrownBy(() -> userService.userUpdate(UserFixture.TEST_USER_ID, dto))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // updateUserAffiliation()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("소속 배정 성공 - HUB 신규 배정")
    void updateUserAffiliation_hubNew() {
        ReflectionTestUtils.setField(user, "role", Role.HUB_ADMIN);
        ReflectionTestUtils.setField(user, "affiliatedStatus", AffiliatedStatus.UNAFFILIATED);
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        userService.updateUserAffiliation(UserFixture.TEST_USER_ID, Affiliation.HUB, UserFixture.TEST_HUB_ID);

        then(hubService).should().save(user, UserFixture.TEST_HUB_ID);
        assertThat(user.getAffiliatedStatus()).isEqualTo(AffiliatedStatus.HUB_AFFILIATED);
    }

    @Test
    @DisplayName("소속 배정 성공 - HUB 재배정 (허브 변경)")
    void updateUserAffiliation_hubReassign() {
        ReflectionTestUtils.setField(user, "role", Role.HUB_ADMIN);
        ReflectionTestUtils.setField(user, "affiliatedStatus", AffiliatedStatus.HUB_AFFILIATED);
        HubUser hubUser = HubUser.create(user, UserFixture.TEST_HUB_ID);
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));
        given(hubService.findByUser(user)).willReturn(hubUser);

        UUID newHubId = UUID.randomUUID();
        userService.updateUserAffiliation(UserFixture.TEST_USER_ID, Affiliation.HUB, newHubId);

        assertThat(hubUser.getHubId()).isEqualTo(newHubId);
    }

    @Test
    @DisplayName("소속 배정 성공 - COMPANY 신규 배정")
    void updateUserAffiliation_companyNew() {
        ReflectionTestUtils.setField(user, "role", Role.COMPANY_MANAGER);
        ReflectionTestUtils.setField(user, "affiliatedStatus", AffiliatedStatus.UNAFFILIATED);
        UUID companyId = UUID.randomUUID();
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        userService.updateUserAffiliation(UserFixture.TEST_USER_ID, Affiliation.COMPANY, companyId);

        then(companyService).should().save(user, companyId);
        assertThat(user.getAffiliatedStatus()).isEqualTo(AffiliatedStatus.COM_AFFILIATED);
    }

    @Test
    @DisplayName("소속 배정 실패 - NOT_APPLICABLE 상태")
    void updateUserAffiliation_notApplicable() {
        // User 기본 상태가 NOT_APPLICABLE (Role.NONE)
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                userService.updateUserAffiliation(UserFixture.TEST_USER_ID, Affiliation.HUB, UserFixture.TEST_HUB_ID))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("소속 배정 실패 - 역할과 소속 충돌 (HUB 배정인데 COMPANY 역할)")
    void updateUserAffiliation_roleConflict() {
        ReflectionTestUtils.setField(user, "role", Role.COMPANY_MANAGER);
        ReflectionTestUtils.setField(user, "affiliatedStatus", AffiliatedStatus.UNAFFILIATED);
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                userService.updateUserAffiliation(UserFixture.TEST_USER_ID, Affiliation.HUB, UserFixture.TEST_HUB_ID))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.ROLE_AFFILIATION_CONFLICT);
    }

    // ---------------------------------------------------------------
    // getUserInfo()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("유저 정보 조회 성공 - HUB 소속")
    void getUserInfo_hubAffiliated() {
        ReflectionTestUtils.setField(user, "affiliatedStatus", AffiliatedStatus.HUB_AFFILIATED);
        HubUser hubUser = HubUser.create(user, UserFixture.TEST_HUB_ID);
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));
        given(hubService.findByUser(user)).willReturn(hubUser);

        UserDataDto result = userService.getUserInfo(UserFixture.TEST_USER_ID);

        assertThat(result.affiliationId()).isEqualTo(UserFixture.TEST_HUB_ID);
        assertThat(result.affiliatedStatus()).isEqualTo(AffiliatedStatus.HUB_AFFILIATED);
    }

    @Test
    @DisplayName("유저 정보 조회 성공 - COMPANY 소속")
    void getUserInfo_companyAffiliated() {
        UUID companyId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "affiliatedStatus", AffiliatedStatus.COM_AFFILIATED);
        CompanyUser companyUser = CompanyUser.create(user, companyId);
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));
        given(companyService.findByUser(user)).willReturn(companyUser);

        UserDataDto result = userService.getUserInfo(UserFixture.TEST_USER_ID);

        assertThat(result.affiliationId()).isEqualTo(companyId);
        assertThat(result.affiliatedStatus()).isEqualTo(AffiliatedStatus.COM_AFFILIATED);
    }

    @Test
    @DisplayName("유저 정보 조회 성공 - 소속 없음")
    void getUserInfo_unaffiliated() {
        ReflectionTestUtils.setField(user, "affiliatedStatus", AffiliatedStatus.UNAFFILIATED);
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        UserDataDto result = userService.getUserInfo(UserFixture.TEST_USER_ID);

        assertThat(result.affiliationId()).isNull();
    }

    @Test
    @DisplayName("유저 정보 조회 실패 - 존재하지 않는 유저")
    void getUserInfo_userNotFound() {
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserInfo(UserFixture.TEST_USER_ID))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // deleteUser()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 삭제 성공 (soft delete)")
    void deleteUser_success() {
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        userService.deleteUser(UserFixture.TEST_USER_ID, UserFixture.TEST_MASTER_ID);

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getDeletedBy()).isEqualTo(UserFixture.TEST_MASTER_ID);
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 존재하지 않는 유저")
    void deleteUser_userNotFound() {
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(UserFixture.TEST_USER_ID, UserFixture.TEST_MASTER_ID))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // getUserRole()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 역할 조회 성공")
    void getUserRole_success() {
        given(userRepository.findById(UserFixture.TEST_USER_ID)).willReturn(Optional.of(user));

        Role result = userService.getUserRole(UserFixture.TEST_USER_ID);

        assertThat(result).isEqualTo(user.getRole());
    }

    @Test
    @DisplayName("사용자 역할 조회 실패 - 존재하지 않는 유저")
    void getUserRole_userNotFound() {
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserRole(UserFixture.TEST_USER_ID))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}
