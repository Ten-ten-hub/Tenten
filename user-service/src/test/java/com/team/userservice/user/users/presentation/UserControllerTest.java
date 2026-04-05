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
import com.team.userservice.user.core.enums.Affiliation;
import com.team.userservice.user.core.enums.AffiliatedStatus;
import com.team.userservice.user.core.enums.Role;
import com.team.userservice.user.users.application.UserService;
import com.team.userservice.user.users.presentation.dto.request.SignUpReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserAffiliationReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserReq;
import com.team.userservice.user.users.presentation.dto.request.UpdateUserRoleReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({PasswordEncoderConfig.class, RoleCheckAspect.class, GlobalExceptionHandler.class, PageableConfig.class, AopAutoConfiguration.class})
class UserControllerTest extends AbstractRestDocsTest {

    @MockBean
    private UserService userService;

    // ---------------------------------------------------------------
    // 1. POST /api/v1/users/signup
    // ---------------------------------------------------------------

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() throws Exception {
        SignUpReq request = new SignUpReq(
                UserFixture.TEST_NAME,
                UserFixture.TEST_LOGIN_ID,
                UserFixture.TEST_PASSWORD,
                UserFixture.TEST_SLACK_ID,
                UserFixture.TEST_EMAIL,
                UserFixture.TEST_PHONE
        );

        given(userService.signUp(any())).willReturn(UserFixture.createSignUpResultDto());

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("users/signup-success",
                        requestFields(
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("loginId").description("로그인 아이디 (4~10자, 영문 소문자+숫자)"),
                                fieldWithPath("password").description("비밀번호 (8~15자, 대/소문자/숫자/특수문자 포함)"),
                                fieldWithPath("slackId").description("슬랙 아이디"),
                                fieldWithPath("email").description("이메일"),
                                fieldWithPath("phoneNumber").description("전화번호")
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과 (SUCCESS)"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data.userId").description("생성된 유저 ID"),
                                fieldWithPath("data.loginId").description("로그인 아이디"),
                                fieldWithPath("data.name").description("이름"),
                                fieldWithPath("data.role").description("역할 (초기값: NONE)"),
                                fieldWithPath("data.email").description("이메일"),
                                fieldWithPath("data.signupStatus").description("가입 상태 (초기값: PENDING)"),
                                fieldWithPath("data.createdAt").description("가입 요청 시각")
                        )
                ));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 로그인 아이디")
    void signUp_duplicateLoginId() throws Exception {
        SignUpReq request = new SignUpReq(
                UserFixture.TEST_NAME,
                UserFixture.TEST_LOGIN_ID,
                UserFixture.TEST_PASSWORD,
                UserFixture.TEST_SLACK_ID,
                UserFixture.TEST_EMAIL,
                UserFixture.TEST_PHONE
        );

        given(userService.signUp(any())).willThrow(new UserException(UserErrorCode.DUPLICATE_LOGIN_ID));

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409_1"))
                .andDo(document("users/signup-duplicate-login-id",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보 (없으면 null)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검사 실패")
    void signUp_invalidInput() throws Exception {
        SignUpReq request = new SignUpReq(
                "",         // name 공백
                "ab",       // loginId 4자 미만
                "weak",     // password 규칙 위반
                UserFixture.TEST_SLACK_ID,
                "not-an-email",
                UserFixture.TEST_PHONE
        );

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(document("users/signup-invalid-input",
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
    // 2. PATCH /api/v1/users/{userId}/registration
    // ---------------------------------------------------------------

    @Test
    @DisplayName("가입 승인 성공")
    void register_success() throws Exception {
        willDoNothing().given(userService).register(any(UUID.class));

        mockMvc.perform(patch("/api/v1/users/{userId}/registration", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andDo(document("users/register-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
                        pathParameters(
                                parameterWithName("userId").description("승인할 유저 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data").type(JsonFieldType.STRING).description("처리 결과 메시지")
                        )
                ));
    }

    @Test
    @DisplayName("가입 승인 실패 - 권한 없음")
    void register_forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{userId}/registration", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "HUB_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_403"))
                .andDo(document("users/register-forbidden",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    @Test
    @DisplayName("가입 승인 실패 - 존재하지 않는 유저")
    void register_notFound() throws Exception {
        willThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .given(userService).register(any(UUID.class));

        mockMvc.perform(patch("/api/v1/users/{userId}/registration", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404"))
                .andDo(document("users/register-not-found",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 3. PATCH /api/v1/users/{userId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 정보 수정 성공")
    void updateUser_success() throws Exception {
        UpdateUserReq request = new UpdateUserReq("김철수", null, null, null, null);
        willDoNothing().given(userService).userUpdate(any(UUID.class), any());

        mockMvc.perform(patch("/api/v1/users/{userId}", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("users/update-user-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
                        pathParameters(
                                parameterWithName("userId").description("수정할 유저 ID")
                        ),
                        requestFields(
                                fieldWithPath("name").description("변경할 이름 (null 이면 변경 안 함)").optional(),
                                fieldWithPath("loginId").description("변경할 로그인 아이디").optional(),
                                fieldWithPath("email").description("변경할 이메일").optional(),
                                fieldWithPath("phoneNumber").description("변경할 전화번호").optional(),
                                fieldWithPath("slackId").description("변경할 슬랙 아이디").optional()
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
    // 4. PATCH /api/v1/users/{userId}/role
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 권한 수정 성공")
    void updateUserRole_success() throws Exception {
        UpdateUserRoleReq request = new UpdateUserRoleReq(Role.HUB_ADMIN);
        willDoNothing().given(userService).updateUserRole(any(UUID.class), any(Role.class));

        mockMvc.perform(patch("/api/v1/users/{userId}/role", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("users/update-user-role-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
                        pathParameters(
                                parameterWithName("userId").description("수정할 유저 ID")
                        ),
                        requestFields(
                                fieldWithPath("role").description("변경할 역할 (NONE, MASTER_ADMIN, HUB_ADMIN, HUB_DELIVERY_MANAGER, COM_DELIVERY_MANAGER, COMPANY_MANAGER)")
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
    // 5. PATCH /api/v1/users/{userId}/affiliation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 소속 배정 성공")
    void updateUserAffiliation_success() throws Exception {
        UpdateUserAffiliationReq request = new UpdateUserAffiliationReq(Affiliation.HUB, UserFixture.TEST_HUB_ID);
        willDoNothing().given(userService).updateUserAffiliation(any(UUID.class), any(Affiliation.class), any(UUID.class));

        mockMvc.perform(patch("/api/v1/users/{userId}/affiliation", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("users/update-affiliation-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
                        pathParameters(
                                parameterWithName("userId").description("소속 배정할 유저 ID")
                        ),
                        requestFields(
                                fieldWithPath("affiliation").description("소속 유형 (HUB, COMPANY, NONE)"),
                                fieldWithPath("affiliationId").description("소속 ID (허브 또는 업체 UUID)")
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
    // 6. GET /api/v1/users/{userId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 단건 조회 성공")
    void getUserInfo_success() throws Exception {
        given(userService.getUserInfo(any(UUID.class))).willReturn(UserFixture.createUserDataDto());

        mockMvc.perform(get("/api/v1/users/{userId}", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(UserFixture.TEST_USER_ID.toString()))
                .andDo(document("users/get-user-info-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
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
    void getUserInfo_notFound() throws Exception {
        given(userService.getUserInfo(any(UUID.class)))
                .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/{userId}", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404"))
                .andDo(document("users/get-user-info-not-found",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지"),
                                fieldWithPath("details").description("상세 정보").optional()
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 7. GET /api/v1/users
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 전체 조회 성공 (필터 없음)")
    void getAllUserInfo_success() throws Exception {
        User user = UserFixture.createUser();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        given(userService.getAllUserInfo(any())).willReturn(page);

        mockMvc.perform(get("/api/v1/users")
                        .header("X-User-Role", "MASTER_ADMIN")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].loginId").value(UserFixture.TEST_LOGIN_ID))
                .andDo(document("users/get-all-user-info-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기 (10, 30, 50 허용)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data.content[].userId").description("유저 ID"),
                                fieldWithPath("data.content[].role").description("역할"),
                                fieldWithPath("data.content[].signupStatus").description("가입 상태"),
                                fieldWithPath("data.content[].name").description("이름"),
                                fieldWithPath("data.content[].loginId").description("로그인 아이디"),
                                fieldWithPath("data.content[].email").description("이메일"),
                                fieldWithPath("data.content[].phoneNumber").description("전화번호"),
                                fieldWithPath("data.content[].slackId").description("슬랙 아이디"),
                                fieldWithPath("data.content[].affiliatedStatus").description("소속 상태"),
                                fieldWithPath("data.content[].lastLoginAt").description("마지막 로그인 시각").optional(),
                                fieldWithPath("data.content[].createdAt").description("가입 시각"),
                                fieldWithPath("data.content[].updatedAt").description("최종 수정 시각"),
                                subsectionWithPath("data.pageable").description("페이지 정보"),
                                fieldWithPath("data.totalElements").description("전체 요소 수"),
                                fieldWithPath("data.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.number").description("현재 페이지 번호"),
                                fieldWithPath("data.size").description("페이지 크기"),
                                fieldWithPath("data.first").description("첫 번째 페이지 여부"),
                                fieldWithPath("data.last").description("마지막 페이지 여부"),
                                fieldWithPath("data.empty").description("빈 페이지 여부"),
                                fieldWithPath("data.numberOfElements").description("현재 페이지 요소 수"),
                                subsectionWithPath("data.sort").description("정렬 정보")
                        )
                ));
    }

    @Test
    @DisplayName("사용자 전체 조회 성공 (필터 적용)")
    void getAllUserInfo_withFilter() throws Exception {
        User user = UserFixture.createUser();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        given(userService.getAllUserInfo(any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/api/v1/users")
                        .header("X-User-Role", "MASTER_ADMIN")
                        .param("roles", "HUB_ADMIN")
                        .param("affiliatedStatus", "UNAFFILIATED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(document("users/get-all-user-info-with-filter",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
                        queryParameters(
                                parameterWithName("roles").description("역할 필터 (복수 가능, 예: HUB_ADMIN,COMPANY_MANAGER)").optional(),
                                parameterWithName("affiliatedStatus").description("소속 상태 필터 (UNAFFILIATED, HUB_AFFILIATED, COM_AFFILIATED, NOT_APPLICABLE)").optional(),
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        )
                ));
    }

    // ---------------------------------------------------------------
    // 8. DELETE /api/v1/users/{userId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("사용자 삭제 성공")
    void deleteUser_success() throws Exception {
        willDoNothing().given(userService).deleteUser(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/api/v1/users/{userId}", UserFixture.TEST_USER_ID)
                        .header("X-User-Role", "MASTER_ADMIN")
                        .header("X-User-Id", UserFixture.TEST_MASTER_ID.toString()))
                .andExpect(status().isOk())
                .andDo(document("users/delete-user-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)"),
                                headerWithName("X-User-Id").description("삭제 요청자 ID (soft delete 기록용)")
                        ),
                        pathParameters(
                                parameterWithName("userId").description("삭제할 유저 ID")
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
    // 9. GET /api/v1/users/me
    // ---------------------------------------------------------------

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_success() throws Exception {
        given(userService.getUserInfo(any(UUID.class))).willReturn(UserFixture.createUserDataDto());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-User-Id", UserFixture.TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value(UserFixture.TEST_LOGIN_ID))
                .andDo(document("users/get-my-info-success",
                        requestHeaders(
                                headerWithName("X-User-Id").description("인증된 유저 ID (게이트웨이 전달)")
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

    // ---------------------------------------------------------------
    // 10. PATCH /api/v1/users/me
    // ---------------------------------------------------------------

    @Test
    @DisplayName("내 정보 수정 성공")
    void updateMe_success() throws Exception {
        UpdateUserReq request = new UpdateUserReq(null, null, "new@example.com", null, null);
        willDoNothing().given(userService).userUpdate(any(UUID.class), any());

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-User-Id", UserFixture.TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("users/update-me-success",
                        requestHeaders(
                                headerWithName("X-User-Id").description("인증된 유저 ID (게이트웨이 전달)")
                        ),
                        requestFields(
                                fieldWithPath("name").description("변경할 이름").optional(),
                                fieldWithPath("loginId").description("변경할 로그인 아이디").optional(),
                                fieldWithPath("email").description("변경할 이메일").optional(),
                                fieldWithPath("phoneNumber").description("변경할 전화번호").optional(),
                                fieldWithPath("slackId").description("변경할 슬랙 아이디").optional()
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
    // 11. GET /api/v1/users/pending
    // ---------------------------------------------------------------

    @Test
    @DisplayName("가입 승인 대기 목록 조회 성공")
    void getPendingUserInfo_success() throws Exception {
        User user = UserFixture.createUser();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        given(userService.getAllUserInfoBySignUpStatus(any(), any())).willReturn(page);

        mockMvc.perform(get("/api/v1/users/pending")
                        .header("X-User-Role", "MASTER_ADMIN")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(document("users/get-pending-users-success",
                        requestHeaders(
                                headerWithName("X-User-Role").description("요청자 역할 (MASTER_ADMIN 필요)")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").description("처리 결과"),
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시각"),
                                fieldWithPath("data.content[].userId").description("유저 ID"),
                                fieldWithPath("data.content[].role").description("역할"),
                                fieldWithPath("data.content[].signupStatus").description("가입 상태 (PENDING)"),
                                fieldWithPath("data.content[].name").description("이름"),
                                fieldWithPath("data.content[].loginId").description("로그인 아이디"),
                                fieldWithPath("data.content[].email").description("이메일"),
                                fieldWithPath("data.content[].phoneNumber").description("전화번호"),
                                fieldWithPath("data.content[].slackId").description("슬랙 아이디"),
                                fieldWithPath("data.content[].affiliatedStatus").description("소속 상태"),
                                fieldWithPath("data.content[].lastLoginAt").description("마지막 로그인 시각").optional(),
                                fieldWithPath("data.content[].createdAt").description("가입 요청 시각"),
                                fieldWithPath("data.content[].updatedAt").description("최종 수정 시각"),
                                subsectionWithPath("data.pageable").description("페이지 정보"),
                                fieldWithPath("data.totalElements").description("전체 대기 수"),
                                fieldWithPath("data.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.number").description("현재 페이지 번호"),
                                fieldWithPath("data.size").description("페이지 크기"),
                                fieldWithPath("data.first").description("첫 번째 페이지 여부"),
                                fieldWithPath("data.last").description("마지막 페이지 여부"),
                                fieldWithPath("data.empty").description("빈 페이지 여부"),
                                fieldWithPath("data.numberOfElements").description("현재 페이지 요소 수"),
                                subsectionWithPath("data.sort").description("정렬 정보")
                        )
                ));
    }
}