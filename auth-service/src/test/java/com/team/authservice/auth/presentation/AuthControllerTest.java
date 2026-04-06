package com.team.authservice.auth.presentation;

import com.team.authservice.auth.application.AuthService;
import com.team.authservice.auth.security.jwt.JwtProperties;
import com.team.authservice.auth.presentation.dto.request.LoginReq;
import com.team.authservice.global.config.SecurityConfig;
import com.team.authservice.global.error.AuthErrorCode;
import com.team.authservice.global.exception.AuthException;
import com.team.authservice.support.AbstractRestDocsTest;
import com.team.authservice.support.AuthFixture;
import com.team.common.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.cookies.CookieDocumentation.responseCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
class AuthControllerTest extends AbstractRestDocsTest {

    @MockBean
    private AuthService authService;

    // ---------------------------------------------------------------
    // 1. POST /api/v1/auth/login
    // ---------------------------------------------------------------

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        LoginReq request = new LoginReq(AuthFixture.TEST_LOGIN_ID, AuthFixture.TEST_PASSWORD);
        given(authService.login(anyString(), anyString())).willReturn(AuthFixture.createTokenDto());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value(AuthFixture.ACCESS_TOKEN))
                .andDo(document("auth/login-success",
                        requestFields(
                                fieldWithPath("loginId").description("로그인 아이디"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseCookies(
                                cookieWithName("refreshToken").description("리프레시 토큰 (HttpOnly 쿠키, 7일 유효)")
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과 (SUCCESS)"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data.accessToken").description("발급된 액세스 토큰")
                        )
                ));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 자격증명")
    void login_invalidCredentials() throws Exception {
        LoginReq request = new LoginReq(AuthFixture.TEST_LOGIN_ID, "wrongPassword");
        given(authService.login(anyString(), anyString()))
                .willThrow(new AuthException(AuthErrorCode.INVALID_TOKEN));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andDo(document("auth/login-invalid-credentials",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 2. DELETE /api/v1/auth/logout
    // ---------------------------------------------------------------

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        willDoNothing().given(authService).logout(any());

        mockMvc.perform(delete("/api/v1/auth/logout")
                        .header("X-User-Id", AuthFixture.TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("auth/logout-success",
                        requestHeaders(
                                headerWithName("X-User-Id").description("로그아웃할 유저 ID (게이트웨이 전달)")
                        ),
                        responseCookies(
                                cookieWithName("refreshToken").description("만료된 리프레시 토큰 쿠키 (maxAge=0)")
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 (없음)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("로그아웃 실패 - X-User-Id 헤더 없음")
    void logout_missingHeader() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andDo(document("auth/logout-missing-header"));
    }

    // ---------------------------------------------------------------
    // 3. PATCH /api/v1/auth/refresh
    // ---------------------------------------------------------------

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_success() throws Exception {
        given(authService.refresh(anyString())).willReturn(AuthFixture.createNewTokenDto());

        mockMvc.perform(patch("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", AuthFixture.REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(AuthFixture.NEW_ACCESS_TOKEN))
                .andDo(document("auth/refresh-success",
                        requestCookies(
                                cookieWithName("refreshToken").description("기존 리프레시 토큰")
                        ),
                        responseCookies(
                                cookieWithName("refreshToken").description("새로 발급된 리프레시 토큰 (HttpOnly 쿠키)")
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data.accessToken").description("새로 발급된 액세스 토큰")
                        )
                ));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void refresh_invalidToken() throws Exception {
        given(authService.refresh(anyString()))
                .willThrow(new AuthException(AuthErrorCode.INVALID_TOKEN));

        mockMvc.perform(patch("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andDo(document("auth/refresh-invalid-token",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 쿠키 없음")
    void refresh_noCookie() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andDo(document("auth/refresh-no-cookie"));
    }
}