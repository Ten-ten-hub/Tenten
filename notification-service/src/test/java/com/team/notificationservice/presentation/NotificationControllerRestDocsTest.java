package com.team.notificationservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
class NotificationControllerRestDocsTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            // Pageable 파라미터를 처리하기 위한 리졸버 등록
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .apply(documentationConfiguration(restDocumentation))
            .build();
    }

    @Test
    @DisplayName("슬랙 알림 생성 및 발송 API 문서화")
    void sendNotification() throws Exception {
        NotificationRequest request = new NotificationRequest(
            "U12345678", "test@team.com", UUID.randomUUID(), "테스트 알림입니다.", MsgType.ORDER_ALERT
        );

        mockMvc.perform(post("/api/v1/notifications/slack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(document("notifications/create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("receiverSlackId").description("수신자 슬랙 ID"),
                    fieldWithPath("email").description("수신자 이메일").optional(),
                    fieldWithPath("orderId").description("연관 주문 ID").optional(),
                    fieldWithPath("message").description("알림 메시지 본문"),
                    fieldWithPath("msgType").description("메시지 타입")
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
    @DisplayName("알림 목록 조회 및 검색 API 문서화")
    void getNotifications() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
            .id(UUID.randomUUID())
            .message("검색된 메시지")
            .status(SendStatus.SUCCESS)
            .createdAt(LocalDateTime.now())
            .build();

        given(notificationService.searchNotifications(any(), any()))
            .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/notifications")
                .param("slackId", "U12345678")
                .param("keyword", "테스트")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andDo(document("notifications/list",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("slackId").description("조회 대상 슬랙 ID"),
                    parameterWithName("keyword").description("검색 키워드").optional(),
                    parameterWithName("page").description("페이지 번호").optional(),
                    parameterWithName("size").description("페이지 크기").optional()
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    // Page 데이터 구조
                    fieldWithPath("data.content[].id").description("알림 ID"),
                    fieldWithPath("data.content[].message").description("알림 내용"),
                    fieldWithPath("data.content[].status").description("전송 상태"),
                    fieldWithPath("data.content[].createdAt").description("생성 시간"),
                    // pageable 내부의 모든 필드 명시 또는 무시
                    fieldWithPath("data.pageable.pageNumber").ignored(),
                    fieldWithPath("data.pageable.pageSize").ignored(),
                    fieldWithPath("data.pageable.sort.sorted").ignored(),
                    fieldWithPath("data.pageable.sort.unsorted").ignored(),
                    fieldWithPath("data.pageable.sort.empty").ignored(),
                    fieldWithPath("data.pageable.offset").ignored(),
                    fieldWithPath("data.pageable.paged").ignored(),
                    fieldWithPath("data.pageable.unpaged").ignored(),
                    fieldWithPath("data.totalElements").description("전체 데이터 수"),
                    fieldWithPath("data.totalPages").description("전체 페이지 수"),
                    fieldWithPath("data.last").description("마지막 페이지 여부"),
                    fieldWithPath("data.size").description("페이지 크기"),
                    fieldWithPath("data.number").description("현재 페이지 번호"),
                    fieldWithPath("data.sort.sorted").description("정렬 여부"),
                    fieldWithPath("data.sort.unsorted").description("비정렬 여부"),
                    fieldWithPath("data.sort.empty").description("정렬 정보 비어있음 여부"),
                    fieldWithPath("data.first").description("첫 페이지 여부"),
                    fieldWithPath("data.numberOfElements").description("현재 페이지 데이터 수"),
                    fieldWithPath("data.empty").description("데이터 비어있음 여부")
                )
            ));
    }

    @Test
    @DisplayName("알림 단건 조회 API 문서화")
    void getNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        NotificationResponse response = NotificationResponse.builder()
            .id(notificationId)
            .message("단건 조회 메시지")
            .status(SendStatus.SUCCESS)
            .createdAt(LocalDateTime.now())
            .build();

        given(notificationService.getNotification(notificationId)).willReturn(response);

        mockMvc.perform(get("/api/v1/notifications/{id}", notificationId))
            .andExpect(status().isOk())
            .andDo(document("notifications/get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("id").description("알림 고유 ID")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("data.id").description("알림 ID"),
                    fieldWithPath("data.message").description("알림 내용"),
                    fieldWithPath("data.status").description("전송 상태"),
                    fieldWithPath("data.createdAt").description("생성 시간"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지")
                )
            ));
    }

    @Test
    @DisplayName("알림 삭제 API 문서화")
    void deleteNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        doNothing().when(notificationService).deleteNotification(eq(notificationId), any());

        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                .header("X-User-Id", "ADMIN_USER"))
            .andExpect(status().isNoContent())
            .andDo(document("notifications/delete",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("id").description("삭제할 알림 ID")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("data").description("데이터 없음").optional(),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지")
                )
            ));
    }
}
