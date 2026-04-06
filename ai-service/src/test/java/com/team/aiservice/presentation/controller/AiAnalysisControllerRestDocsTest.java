package com.team.aiservice.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.team.aiservice.application.dto.AiRequest;
import com.team.aiservice.application.service.AiAnalysisService;
import com.team.aiservice.domain.model.AiAnalysis;
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
public class AiAnalysisControllerRestDocsTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AiAnalysisService aiAnalysisService;
    @InjectMocks
    private AiAnalysisController aiAnalysisController;
    @InjectMocks
    private InternalAiController internalAiController;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.standaloneSetup(aiAnalysisController, internalAiController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .apply(documentationConfiguration(restDocumentation))
            .build();
    }

    @Test
    @DisplayName("내부 API: AI 분석 생성 문서화")
    void internalCreate_Docs() throws Exception {
        AiRequest req = new AiRequest(UUID.randomUUID(), "TV", UUID.randomUUID(), "H1", "Addr1",
            UUID.randomUUID(), "H2", "Addr2", "req", UUID.randomUUID(), "slack", "09-18");
        AiAnalysis res = AiAnalysis.builder().id(UUID.randomUUID()).aiResult("분석 완료").build();
        given(aiAnalysisService.analyzeDeadline(any())).willReturn(res);

        mockMvc.perform(post("/internal/v1/ais/analysis")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andDo(document("ais/internal-create",
                requestHeaders(headerWithName("X-Internal-Request").description("시스템 내부 호출 인증 헤더")),
                requestFields(
                    fieldWithPath("orderId").description("주문 ID"),
                    fieldWithPath("productName").description("상품명"),
                    fieldWithPath("originHubId").description("출발 허브 ID"),
                    fieldWithPath("originHubName").description("출발 허브 이름"),
                    fieldWithPath("originAddress").description("출발지 주소"),
                    fieldWithPath("destinationHubId").description("도착 허브 ID"),
                    fieldWithPath("destinationHubName").description("도착 허브 이름"),
                    fieldWithPath("destinationAddress").description("도착지 주소"),
                    fieldWithPath("orderRequestDetails").description("주문 요청 사항"),
                    fieldWithPath("receiverId").description("수신자 ID"),
                    fieldWithPath("receiverSlackId").description("수신자 슬랙 ID"),
                    fieldWithPath("workingHours").description("근무 시간")
                ),
                responseFields(
                    fieldWithPath("aiAnalysisId").description("생성된 AI 분석 기록 ID"),
                    fieldWithPath("aiResult").description("AI 분석 결과 본문")
                )
            ));
    }

    @Test
    @DisplayName("목록 조회: AI 분석 기록 리스트 문서화")
    void listAnalyses_Docs() throws Exception {
        AiAnalysis analysis = AiAnalysis.builder().id(UUID.randomUUID()).aiResult("결과내용").build();
        given(aiAnalysisService.search(any(), any())).willReturn(new PageImpl<>(List.of(analysis)));

        mockMvc.perform(
                get("/api/v1/ais").param("page", "1").param("size", "10").param("orderId", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andDo(document("ais/list",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("page").description("페이지 번호 (1부터 시작)"),
                    parameterWithName("size").description("한 페이지당 개수"),
                    parameterWithName("orderId").description("특정 주문 ID로 검색 (필터)").optional()
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("상태 코드"),
                    fieldWithPath("message").description("결과 메시지"),
                    fieldWithPath("data.content[].aiAnalysisId").description("기록 ID"),
                    fieldWithPath("data.content[].aiResult").description("분석 결과"),
                    fieldWithPath("data.pageInfo.currentPage").description("현재 페이지"),
                    fieldWithPath("data.pageInfo.size").description("페이지 크기"),
                    fieldWithPath("data.pageInfo.totalElements").description("총 데이터 개수"),
                    fieldWithPath("data.pageInfo.totalPages").description("총 페이지 개수")
                )
            ));
    }

    @Test
    @DisplayName("단건 조회: 특정 AI 분석 기록 상세 문서화")
    void getAnalysis_Docs() throws Exception {
        UUID id = UUID.randomUUID();
        given(aiAnalysisService.findById(id)).willReturn(AiAnalysis.builder().id(id).aiResult("상세 결과").build());

        mockMvc.perform(get("/api/v1/ais/{id}", id))
            .andExpect(status().isOk())
            .andDo(document("ais/get",
                pathParameters(parameterWithName("id").description("조회할 분석 기록 ID")),
                responseFields(
                    fieldWithPath("success").description("성공"),
                    fieldWithPath("data.aiAnalysisId").description("기록 ID"),
                    fieldWithPath("data.aiResult").description("결과 본문"),
                    fieldWithPath("code").ignored(), fieldWithPath("message").ignored()
                )
            ));
    }

    @Test
    @DisplayName("삭제: AI 분석 기록 소프트 삭제 문서화")
    void deleteAnalysis_Docs() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doNothing().when(aiAnalysisService).softDelete(eq(id), any());

        mockMvc.perform(delete("/api/v1/ais/{id}", id).header("X-User-Id", userId.toString()))
            .andExpect(status().isOk())
            .andDo(document("ais/delete",
                pathParameters(parameterWithName("id").description("삭제할 기록 ID")),
                requestHeaders(headerWithName("X-User-Id").description("요청한 유저 ID")),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("상태 코드"),
                    fieldWithPath("message").description("메시지"),
                    fieldWithPath("data").description("결과 (null)")
                )
            ));
    }
}
