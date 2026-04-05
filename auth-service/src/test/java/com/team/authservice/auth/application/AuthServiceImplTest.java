package com.team.authservice.auth.application;

import com.team.authservice.auth.application.dto.TokenDto;
import com.team.authservice.auth.infrastructure.RedisTokenRepository;
import com.team.authservice.auth.infrastructure.feign.UserInternalClient;
import com.team.authservice.auth.infrastructure.feign.dto.UserRoleRes;
import com.team.authservice.auth.security.jwt.JwtProvider;
import com.team.authservice.core.enums.Role;
import com.team.authservice.global.error.AuthErrorCode;
import com.team.authservice.global.exception.AuthException;
import com.team.authservice.support.AuthFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserInternalClient userInternalClient;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTokenRepository redisTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    // ---------------------------------------------------------------
    // login()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        given(userInternalClient.verify(any())).willReturn(AuthFixture.createUserVerifyRes());
        given(jwtProvider.generateAccessToken(any(), any())).willReturn(AuthFixture.ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(any())).willReturn(AuthFixture.REFRESH_TOKEN);
        willDoNothing().given(redisTokenRepository).save(any(), anyString());

        TokenDto result = authService.login(AuthFixture.TEST_LOGIN_ID, AuthFixture.TEST_PASSWORD);

        assertThat(result.accessToken()).isEqualTo(AuthFixture.ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(AuthFixture.REFRESH_TOKEN);
        then(redisTokenRepository).should().save(AuthFixture.TEST_USER_ID, AuthFixture.REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그인 실패 - user-service 오류 (잘못된 자격증명)")
    void login_feignException() {
        given(userInternalClient.verify(any())).willThrow(new RuntimeException("user-service error"));

        assertThatThrownBy(() -> authService.login(AuthFixture.TEST_LOGIN_ID, "wrongPassword"))
                .isInstanceOf(RuntimeException.class);

        then(jwtProvider).shouldHaveNoInteractions();
        then(redisTokenRepository).shouldHaveNoInteractions();
    }

    // ---------------------------------------------------------------
    // logout()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        willDoNothing().given(redisTokenRepository).delete(any());
        willDoNothing().given(userInternalClient).lastLoginAt(any());

        authService.logout(AuthFixture.TEST_USER_ID);

        then(redisTokenRepository).should().delete(AuthFixture.TEST_USER_ID);
        then(userInternalClient).should().lastLoginAt(AuthFixture.TEST_USER_ID);
    }

    // ---------------------------------------------------------------
    // refresh()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_success() {
        given(jwtProvider.extractUserInfo(AuthFixture.REFRESH_TOKEN)).willReturn(AuthFixture.TEST_USER_ID);
        given(userInternalClient.getUserRole(AuthFixture.TEST_USER_ID)).willReturn(new UserRoleRes(Role.HUB_ADMIN));
        given(jwtProvider.generateAccessToken(any(), any())).willReturn(AuthFixture.NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(any())).willReturn(AuthFixture.NEW_REFRESH_TOKEN);
        given(redisTokenRepository.compareAndReplace(
                AuthFixture.TEST_USER_ID, AuthFixture.REFRESH_TOKEN, AuthFixture.NEW_REFRESH_TOKEN)
        ).willReturn(true);

        TokenDto result = authService.refresh(AuthFixture.REFRESH_TOKEN);

        assertThat(result.accessToken()).isEqualTo(AuthFixture.NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(AuthFixture.NEW_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 토큰 파싱 실패 (만료 또는 위조)")
    void refresh_tokenParseFailed() {
        given(jwtProvider.extractUserInfo(anyString())).willThrow(new RuntimeException("invalid token"));

        assertThatThrownBy(() -> authService.refresh("invalid-token"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);

        then(userInternalClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 역할 조회 중 user-service 오류")
    void refresh_getUserRoleFailed() {
        given(jwtProvider.extractUserInfo(AuthFixture.REFRESH_TOKEN)).willReturn(AuthFixture.TEST_USER_ID);
        given(userInternalClient.getUserRole(any())).willThrow(new RuntimeException("feign error"));

        assertThatThrownBy(() -> authService.refresh(AuthFixture.REFRESH_TOKEN))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis 토큰 불일치 (재사용 또는 동시 요청)")
    void refresh_compareAndReplaceFailed() {
        given(jwtProvider.extractUserInfo(AuthFixture.REFRESH_TOKEN)).willReturn(AuthFixture.TEST_USER_ID);
        given(userInternalClient.getUserRole(any())).willReturn(new UserRoleRes(Role.HUB_ADMIN));
        given(jwtProvider.generateAccessToken(any(), any())).willReturn(AuthFixture.NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(any())).willReturn(AuthFixture.NEW_REFRESH_TOKEN);
        given(redisTokenRepository.compareAndReplace(any(), anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> authService.refresh(AuthFixture.REFRESH_TOKEN))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }
}