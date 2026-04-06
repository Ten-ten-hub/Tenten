package com.team.notificationservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
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
import com.team.notificationservice.presentation.common.NotificationExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
public class NotificationControllerRestDocsTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 4, 7, 10, 0, 0);

    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
            .setControllerAdvice(new NotificationExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .apply(documentationConfiguration(restDocumentation))
            .build();
    }

    @Test
    @DisplayName("알림 목록 조회 API 문서화")
    void listNotifications_Docs() throws Exception {
        NotificationResponse res = new NotificationResponse(FIXED_ID, "결과 메시지 내용", SendStatus.SUCCESS, FIXED_NOW);
        given(notificationService.searchNotifications(any(), any())).willReturn(new PageImpl<>(List.of(res)));

        mockMvc.perform(get("/api/v1/notifications")
                .param("slackId", "U12345")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andDo(document("notifications/list",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("slackId").description("검색할 유저의 슬랙 ID"),
                    parameterWithName("keyword").description("메시지 내용 검색어 (선택)").optional(),
                    parameterWithName("page").description("페이지 번호"),
                    parameterWithName("size").description("페이지 크기")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("상태 코드"),
                    fieldWithPath("message").description("메시지"),
                    fieldWithPath("data.content[].id").description("알림 ID"),
                    fieldWithPath("data.content[].message").description("알림 본문"),
                    fieldWithPath("data.content[].status").description("전송 상태"),
                    fieldWithPath("data.content[].createdAt").description("발송 생성 일시"),
                    fieldWithPath("data.pageInfo.currentPage").description("현재 페이지"),
                    fieldWithPath("data.pageInfo.size").description("페이지 크기"),
                    fieldWithPath("data.pageInfo.totalElements").description("전체 알림 개수"),
                    fieldWithPath("data.pageInfo.totalPages").description("전체 페이지 개수")
                )
            ));
    }

    @Test
    @DisplayName("슬랙 알림 생성 API 문서화")
    void createNotification_Docs() throws Exception {
        NotificationRequest req = new NotificationRequest("U12345", "test@test.com", FIXED_ID, "알림 메시지",
            MsgType.ORDER_ALERT);
        doNothing().when(notificationService).createAndSend(any(), any());

        mockMvc.perform(post("/api/v1/notifications/slack")
                .header("X-User-Id", FIXED_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andDo(document("notifications/create",
                requestHeaders(headerWithName("X-User-Id").description("요청 유저 ID")),
                requestFields(
                    fieldWithPath("receiverSlackId").description("수신자 슬랙 ID (선택)").optional(),
                    fieldWithPath("email").description("수신자 이메일 (슬랙 ID 없을 때 필수)").optional(),
                    fieldWithPath("orderId").description("연관 주문 ID"),
                    fieldWithPath("message").description("알림 내용"),
                    fieldWithPath("msgType").description("메시지 타입 (ORDER_ALERT 등)"),
                    fieldWithPath("validRecipient").ignored()
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("상태 코드"),
                    fieldWithPath("message").description("메시지"),
                    fieldWithPath("data").description("결과 메시지")
                )
            ));
    }

    @Test
    @DisplayName("알림 단건 조회 API 문서화")
    void getNotification_Docs() throws Exception {
        NotificationResponse res = new NotificationResponse(FIXED_ID, "알림 상세 내용", SendStatus.SUCCESS, FIXED_NOW);
        given(notificationService.getNotification(FIXED_ID)).willReturn(res);

        mockMvc.perform(get("/api/v1/notifications/{id}", FIXED_ID))
            .andExpect(status().isOk())
            .andDo(document("notifications/get",
                pathParameters(parameterWithName("id").description("조회할 알림 ID")),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("data.id").description("알림 ID"),
                    fieldWithPath("data.message").description("알림 본문"),
                    fieldWithPath("data.status").description("상태"),
                    fieldWithPath("data.createdAt").description("생성일시"),
                    fieldWithPath("code").ignored(), fieldWithPath("message").ignored()
                )
            ));
    }

    @Test
    @DisplayName("알림 삭제 API 문서화")
    void deleteNotification_Docs() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/{id}", FIXED_ID)
                .header("X-User-Id", FIXED_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andExpect(status().isOk())
            .andDo(document("notifications/delete",
                pathParameters(parameterWithName("id").description("삭제할 알림 ID")),
                requestHeaders(
                    headerWithName("X-User-Id").description("요청 유저 ID"),
                    headerWithName("X-User-Role").description("사용자 권한 (MASTER_ADMIN 필요)")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("상태 코드"),
                    fieldWithPath("message").description("메시지"),
                    fieldWithPath("data").description("결과 (null)")
                )
            ));
    }
}
