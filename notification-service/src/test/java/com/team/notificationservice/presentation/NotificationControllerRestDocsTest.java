package com.team.notificationservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.domain.MsgType;
import com.team.notificationservice.domain.SendStatus;
import com.team.notificationservice.presentation.common.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
class NotificationControllerRestDocsTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .apply(documentationConfiguration(restDocumentation))
            .build();
    }

    @Test
    @DisplayName("슬랙 알림 생성 API 문서화")
    void sendNotification() throws Exception {
        NotificationRequest request = new NotificationRequest(
            "U12345678", "test@team.com", UUID.randomUUID(), "테스트 메시지", MsgType.ORDER_ALERT
        );

        doNothing().when(notificationService).createAndSend(any(NotificationRequest.class));

        mockMvc.perform(post("/api/v1/notifications/slack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(document("notifications/create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("receiverSlackId").description("수신자 슬랙 ID (이메일과 둘 중 하나 필수)").optional(),
                    fieldWithPath("email").description("수신자 이메일 (슬랙 ID와 둘 중 하나 필수)").optional(),
                    fieldWithPath("orderId").description("연관 주문 ID").optional(),
                    fieldWithPath("message").description("알림 메시지 본문"),
                    fieldWithPath("msgType").description("메시지 타입"),
                    // @AssertTrue 검증 필드에 대한 문서화 누락 해결
                    fieldWithPath("validRecipient").description("수신자 유효성 체크 필드 (내부 검증용)").ignored()
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("data").description("응답 데이터").optional(),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지")
                )
            ));
    }

    @Test
    @DisplayName("알림 생성 실패 문서화 (Validation 에러)")
    void sendNotification_Fail() throws Exception {
        // 모든 필드가 비어있어 수신자 검증 및 필수값 검증에서 실패하는 요청
        NotificationRequest invalidRequest = new NotificationRequest(null, null, null, null, null);

        mockMvc.perform(post("/api/v1/notifications/slack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andDo(document("notifications/create-fail",
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("success").description("성공 여부 (false)"),
                    fieldWithPath("code").description("에러 코드"),
                    fieldWithPath("message").description("에러 메시지"),
                    fieldWithPath("data").description("응답 데이터 (null)").optional(),
                    fieldWithPath("errors").type(JsonFieldType.ARRAY).description("상세 에러 목록"),
                    fieldWithPath("errors[].field").description("에러 발생 필드"),
                    fieldWithPath("errors[].value").description("잘못 입력된 값"),
                    fieldWithPath("errors[].reason").description("에러 원인")
                )
            ));
    }

    @Test
    @DisplayName("알림 목록 조회 API 문서화")
    void getNotifications() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
            .id(UUID.randomUUID()).message("메시지").status(SendStatus.SUCCESS).createdAt(LocalDateTime.now()).build();

        given(notificationService.searchNotifications(any(), any()))
            .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/notifications")
                .param("slackId", "U12345678")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andDo(document("notifications/list",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("slackId").description("슬랙 ID"),
                    parameterWithName("page").description("페이지").optional(),
                    parameterWithName("size").description("사이즈").optional()
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("코드"),
                    fieldWithPath("message").description("메시지"),
                    fieldWithPath("data.content[].id").description("ID"),
                    fieldWithPath("data.content[].message").description("내용"),
                    fieldWithPath("data.content[].status").description("상태"),
                    fieldWithPath("data.content[].createdAt").description("생성일"),
                    // 미문서화 에러 해결: 아래 필드들 추가
                    fieldWithPath("data.pageable.pageNumber").ignored(),
                    fieldWithPath("data.pageable.pageSize").ignored(),
                    fieldWithPath("data.pageable.sort.sorted").ignored(),
                    fieldWithPath("data.pageable.sort.unsorted").ignored(),
                    fieldWithPath("data.pageable.sort.empty").ignored(),
                    fieldWithPath("data.pageable.offset").ignored(),
                    fieldWithPath("data.pageable.paged").ignored(),
                    fieldWithPath("data.pageable.unpaged").ignored(),
                    fieldWithPath("data.totalElements").description("전체 개수"),
                    fieldWithPath("data.totalPages").description("전체 페이지"),
                    fieldWithPath("data.last").description("마지막 여부"),
                    fieldWithPath("data.size").description("사이즈"),
                    fieldWithPath("data.number").description("현재 페이지"),
                    fieldWithPath("data.sort.sorted").ignored(),
                    fieldWithPath("data.sort.unsorted").ignored(),
                    fieldWithPath("data.sort.empty").ignored(),
                    fieldWithPath("data.first").description("첫 페이지 여부"),
                    fieldWithPath("data.numberOfElements").description("현재 페이지 요소 수"),
                    fieldWithPath("data.empty").description("비어있음 여부")
                )
            ));
    }

    @Test
    @DisplayName("알림 단건 조회 API 문서화")
    void getNotification() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationResponse response = NotificationResponse.builder()
            .id(id).message("메시지").status(SendStatus.SUCCESS).createdAt(LocalDateTime.now()).build();

        given(notificationService.getNotification(id)).willReturn(response);

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
            .andExpect(status().isOk())
            .andDo(document("notifications/get",
                pathParameters(parameterWithName("id").description("알림 ID")),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("data.id").description("ID"),
                    fieldWithPath("data.message").description("내용"),
                    fieldWithPath("data.status").description("상태"),
                    fieldWithPath("data.createdAt").description("생성일"),
                    fieldWithPath("code").description("코드"),
                    fieldWithPath("message").description("메시지")
                )
            ));
    }

    @Test
    @DisplayName("알림 삭제 API 문서화")
    void deleteNotification() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(notificationService).deleteNotification(any(), any());

        mockMvc.perform(delete("/api/v1/notifications/{id}", id)
                .header("X-User-Id", "ADMIN"))
            .andExpect(status().isOk()) // ApiResponse.ok()를 쓰므로 200 OK
            .andDo(document("notifications/delete",
                pathParameters(parameterWithName("id").description("삭제할 알림 ID")),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("data").description("데이터 (null)").optional(),
                    fieldWithPath("code").description("코드"),
                    fieldWithPath("message").description("메시지")
                )
            ));
    }

//    @Test
//    @DisplayName("알림 목록 조회 실패 - 결과 없음")
//    void getNotifications_Fail_NotFound() throws Exception {
//        // 검색 결과가 없을 때 ServiceException을 던지는 시나리오 유지
//        given(notificationService.searchNotifications(any(), any()))
//            .willThrow(new ServiceException(ErrorCode.NOTI_NOTIFICATION_NOT_FOUND));
//
//        mockMvc.perform(get("/api/v1/notifications")
//                .param("slackId", "INVALID_ID"))
//            .andExpect(status().isNotFound())
//            .andDo(document("notifications/list-fail",
//                preprocessResponse(prettyPrint()),
//                responseFields(
//                    fieldWithPath("success").description("성공 여부 (false)"),
//                    fieldWithPath("code").description("에러 코드"),
//                    fieldWithPath("message").description("에러 메시지"),
//                    fieldWithPath("data").description("데이터 (null)").optional(),
//                    // .type(JsonFieldType.ARRAY)를 추가하여 타입을 명시
//                    fieldWithPath("errors").type(JsonFieldType.ARRAY).description("상세 에러 목록 (null)").optional()
//                )
//            ));
//    }

    @Test
    @DisplayName("알림 목록 조회 실패 - 슬랙 ID 누락")
    void getNotifications_Fail_InvalidCondition() throws Exception {
        // Validation(@Valid)에 의한 400 에러
        mockMvc.perform(get("/api/v1/notifications")
                // slackId 파라미터를 아예 보내지 않음
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isBadRequest()) // Validation 에러로 400 발생
            .andDo(document("notifications/list-validation-fail",
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("success").description("false"),
                    fieldWithPath("code").description("COMMON_INVALID_INPUT"),
                    fieldWithPath("message").description("입력값이 올바르지 않습니다."),
                    fieldWithPath("data").ignored(),
                    fieldWithPath("errors[].field").description("slackId"),
                    fieldWithPath("errors[].reason").description("조회할 슬랙 ID는 필수입니다."),
                    fieldWithPath("errors[].value").description("null")
                )
            ));
    }

    @Test
    @DisplayName("알림 목록 조회 - 결과가 없을 때 빈 목록 응답 문서화")
    void getNotifications_Empty() throws Exception {
        // PageRequest를 명시하여 500 에러 방지
        given(notificationService.searchNotifications(any(), any()))
            .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/v1/notifications")
                .param("slackId", "U12345678"))
            .andExpect(status().isOk())
            .andDo(document("notifications/list-empty",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("slackId").description("조회할 슬랙 ID")
                ),
                // relaxedResponseFields를 사용하여 명시한 필드 외에는 검증하지 않음
                relaxedResponseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.content").description("빈 결과 리스트"),
                    fieldWithPath("data.totalElements").description("전체 요소 개수"),
                    fieldWithPath("data.totalPages").description("전체 페이지 수"),
                    fieldWithPath("data.number").description("현재 페이지 번호"),
                    fieldWithPath("data.empty").description("비어있음 여부")
                )
            ));
    }
}
