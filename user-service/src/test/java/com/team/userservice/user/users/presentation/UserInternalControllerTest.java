package com.team.userservice.user.users.presentation;

import com.team.common.exception.GlobalExceptionHandler;
import com.team.userservice.global.auth.RoleCheckAspect;
import com.team.userservice.global.config.PageableConfig;
import com.team.userservice.global.config.PasswordEncoderConfig;
import com.team.userservice.global.domain.error.UserErrorCode;
import com.team.userservice.global.exception.UserException;
import com.team.userservice.support.AbstractRestDocsTest;
import com.team.userservice.support.UserFixture;
import com.team.userservice.user.core.User;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.request.LoginReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserInternalController.class)
@Import({PasswordEncoderConfig.class, RoleCheckAspect.class, GlobalExceptionHandler.class, PageableConfig.class, AopAutoConfiguration.class})
class UserInternalControllerTest extends AbstractRestDocsTest {

    @MockBean
    private UserService userService;

    // ---------------------------------------------------------------
    // 1. POST /internal/v1/users/verify
    // ---------------------------------------------------------------

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        LoginReq request = new LoginReq(UserFixture.TEST_LOGIN_ID, UserFixture.TEST_PASSWORD);

        given(userService.loginService(any(), any())).willReturn(UserFixture.createLoginServiceDto());

        mockMvc.perform(post("/internal/v1/users/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(UserFixture.TEST_USER_ID.toString()))
                .andDo(document("internal/login-success",
                        requestFields(
                                fieldWithPath("loginId").description("로그인 아이디"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("userId").description("유저 ID"),
                                fieldWithPath("role").description("유저 역할")
                        )
                ));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 오류")
    void login_invalidCredentials() throws Exception {
        LoginReq request = new LoginReq(UserFixture.TEST_LOGIN_ID, "wrongPassword1!");

        given(userService.loginService(any(), any()))
                .willThrow(new UserException(UserErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/internal/v1/users/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_401"))
                .andDo(document("internal/login-invalid-credentials",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    @Test
    @DisplayName("로그인 실패 - 유효성 검사 실패")
    void login_invalidInput() throws Exception {
        LoginReq request = new LoginReq("", "");

        mockMvc.perform(post("/internal/v1/users/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(document("internal/login-invalid-input",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("필드별 유효성 오류 목록"),
                                fieldWithPath("details[].field").description("오류 필드명"),
                                fieldWithPath("details[].reason").description("오류 사유")
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 2. GET /internal/v1/users
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 전체 조회 성공 (필터 없음)")
    void getAllUserInfoInternal_success() throws Exception {
        User user = UserFixture.createUser();
        given(userService.getAllUserInfoInternal(any(), any())).willReturn(List.of(user));

        mockMvc.perform(get("/internal/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].loginId").value(UserFixture.TEST_LOGIN_ID))
                .andDo(document("internal/get-all-users-success",
                        responseFields(
                                fieldWithPath("result").description("처리 결과"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data[].userId").description("유저 ID"),
                                fieldWithPath("data[].role").description("역할"),
                                fieldWithPath("data[].signupStatus").description("가입 상태"),
                                fieldWithPath("data[].name").description("이름"),
                                fieldWithPath("data[].loginId").description("로그인 아이디"),
                                fieldWithPath("data[].email").description("이메일"),
                                fieldWithPath("data[].phoneNumber").description("전화번호"),
                                fieldWithPath("data[].slackId").description("슬랙 아이디"),
                                fieldWithPath("data[].affiliatedStatus").description("소속 상태"),
                                fieldWithPath("data[].lastLoginAt").description("마지막 로그인 시각").optional(),
                                fieldWithPath("data[].createdAt").description("가입 시각"),
                                fieldWithPath("data[].updatedAt").description("최종 수정 시각")
                        )
                ));
    }

    @Test
    @DisplayName("사용자 전체 조회 성공 (필터 적용)")
    void getAllUserInfoInternal_withFilter() throws Exception {
        User user = UserFixture.createUser();
        given(userService.getAllUserInfoInternal(any(), any())).willReturn(List.of(user));

        mockMvc.perform(get("/internal/v1/users")
                        .param("roles", "HUB_ADMIN")
                        .param("affiliatedStatus", "UNAFFILIATED"))
                .andExpect(status().isOk())
                .andDo(document("internal/get-all-users-with-filter",
                        queryParameters(
                                parameterWithName("roles").description("역할 필터 (복수 가능)").optional(),
                                parameterWithName("affiliatedStatus").description("소속 상태 필터 (UNAFFILIATED, HUB_AFFILIATED, COM_AFFILIATED, NOT_APPLICABLE)").optional()
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 3. GET /internal/v1/users/{userId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 단건 조회 성공")
    void getUserInfoInternal_success() throws Exception {
        given(userService.getUserInfo(any(UUID.class))).willReturn(UserFixture.createUserDataDto());

        mockMvc.perform(get("/internal/v1/users/{userId}", UserFixture.TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(UserFixture.TEST_USER_ID.toString()))
                .andDo(document("internal/get-user-info-success",
                        pathParameters(
                                parameterWithName("userId").description("조회할 유저 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data.userId").description("유저 ID"),
                                fieldWithPath("data.role").description("역할"),
                                fieldWithPath("data.signupStatus").description("가입 상태"),
                                fieldWithPath("data.name").description("이름"),
                                fieldWithPath("data.loginId").description("로그인 아이디"),
                                fieldWithPath("data.email").description("이메일"),
                                fieldWithPath("data.phoneNumber").description("전화번호"),
                                fieldWithPath("data.slackId").description("슬랙 아이디"),
                                fieldWithPath("data.affiliatedStatus").description("소속 상태"),
                                fieldWithPath("data.affiliationId").description("소속 ID (미배정 시 null)").optional(),
                                fieldWithPath("data.lastLoginAt").description("마지막 로그인 시각"),
                                fieldWithPath("data.createdAt").description("가입 시각"),
                                fieldWithPath("data.updatedAt").description("최종 수정 시각")
                        )
                ));
    }

    @Test
    @DisplayName("사용자 단건 조회 실패 - 존재하지 않는 유저")
    void getUserInfoInternal_notFound() throws Exception {
        given(userService.getUserInfo(any(UUID.class)))
                .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/internal/v1/users/{userId}", UserFixture.TEST_USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404"))
                .andDo(document("internal/get-user-info-not-found",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 4. PATCH /internal/v1/users/{userId}/last-login
    // ---------------------------------------------------------------

    @Test
    @DisplayName("마지막 로그인 시각 업데이트 성공")
    void updateLastLoginAt_success() throws Exception {
        willDoNothing().given(userService).updateLastLoginAt(any(UUID.class));

        mockMvc.perform(patch("/internal/v1/users/{userId}/last-login", UserFixture.TEST_USER_ID))
                .andExpect(status().isOk())
                .andDo(document("internal/update-last-login-success",
                        pathParameters(
                                parameterWithName("userId").description("업데이트할 유저 ID")
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

    // ---------------------------------------------------------------
    // 5. GET /internal/v1/users/{userId}/role
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 역할 조회 성공")
    void getUserRoleInternal_success() throws Exception {
        given(userService.getUserRole(any(UUID.class))).willReturn(Role.HUB_ADMIN);

        mockMvc.perform(get("/internal/v1/users/{userId}/role", UserFixture.TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("HUB_ADMIN"))
                .andDo(document("internal/get-user-role-success",
                        pathParameters(
                                parameterWithName("userId").description("조회할 유저 ID")
                        ),
                        responseFields(
                                fieldWithPath("role").description("유저 역할")
                        )
                ));
    }

    @Test
    @DisplayName("사용자 역할 조회 실패 - 존재하지 않는 유저")
    void getUserRoleInternal_notFound() throws Exception {
        given(userService.getUserRole(any(UUID.class)))
                .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/internal/v1/users/{userId}/role", UserFixture.TEST_USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404"))
                .andDo(document("internal/get-user-role-not-found",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 6. GET /internal/v1/users/{userId}/slack
    // ---------------------------------------------------------------

    @Test
    @DisplayName("슬랙 아이디 조회 성공")
    void getSlackInternal_success() throws Exception {
        given(userService.getUserSlackId(any(UUID.class))).willReturn(UserFixture.TEST_SLACK_ID);

        mockMvc.perform(get("/internal/v1/users/{userId}/slack", UserFixture.TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slackId").value(UserFixture.TEST_SLACK_ID))
                .andDo(document("internal/get-slack-id-success",
                        pathParameters(
                                parameterWithName("userId").description("조회할 유저 ID")
                        ),
                        responseFields(
                                fieldWithPath("slackId").description("슬랙 아이디")
                        )
                ));
    }

    @Test
    @DisplayName("슬랙 아이디 조회 실패 - 존재하지 않는 유저")
    void getSlackInternal_notFound() throws Exception {
        given(userService.getUserSlackId(any(UUID.class)))
                .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/internal/v1/users/{userId}/slack", UserFixture.TEST_USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404"))
                .andDo(document("internal/get-slack-id-not-found",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }
}
