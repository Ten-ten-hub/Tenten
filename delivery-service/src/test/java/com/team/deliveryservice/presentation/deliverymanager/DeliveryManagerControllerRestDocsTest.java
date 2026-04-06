package com.team.deliveryservice.presentation.deliverymanager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.deliveryservice.deliverymanager.application.dto.request.CreateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.request.UpdateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerPageResponse;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerResponse;
import com.team.deliveryservice.deliverymanager.application.service.DeliveryManagerService;
import com.team.deliveryservice.deliverymanager.domain.DeliveryManagerType;
import com.team.deliveryservice.deliverymanager.presentation.DeliveryManagerController;
import com.team.deliveryservice.global.config.CurrentUserArgumentResolver;
import com.team.deliveryservice.global.config.CurrentUserResolverConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(DeliveryManagerController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureObservability
@Import({
    CurrentUserArgumentResolver.class,
    CurrentUserResolverConfig.class
})
@TestPropertySource(properties = {
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.docker.compose.enabled=false",
    "management.tracing.enabled=false"
})
class DeliveryManagerControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryManagerService deliveryManagerService;

    private MockHttpServletRequestBuilder withCurrentUser(MockHttpServletRequestBuilder builder) {
        return builder
            .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
            .header("X-User-Role", "MASTER_ADMIN")
            .header("X-Hub-Id", "00000000-0000-0000-0000-000000000002")
            .header("X-Company-Id", "00000000-0000-0000-0000-000000000003");
    }

    private DeliveryManagerResponse mockDeliveryManagerResponse() {
        return DeliveryManagerResponse.builder()
            .deliveryManagerId(UUID.fromString("50000000-0000-0000-0000-000000000001"))
            .hubId(UUID.fromString("40000000-0000-0000-0000-000000000001"))
            .slackId("U12345678")
            .type(DeliveryManagerType.COMPANY_DELIVERY_MANAGER)
            .deliverySequence(1)
            .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
            .updatedAt(null)
            .build();
    }

    private DeliveryManagerPageResponse mockDeliveryManagerPageResponse() {
        return DeliveryManagerPageResponse.builder()
            .content(List.of(mockDeliveryManagerResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .hasNext(false)
            .build();
    }

    @Test
    @DisplayName("배송담당자 생성 API 문서화")
    void createDeliveryManagerDocs() throws Exception {
        CreateDeliveryManagerRequest request = new CreateDeliveryManagerRequest(
            UUID.fromString("50000000-0000-0000-0000-000000000001"),
            UUID.fromString("40000000-0000-0000-0000-000000000001"),
            "U12345678",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        when(deliveryManagerService.createDeliveryManager(any(), any()))
            .thenReturn(mockDeliveryManagerResponse());

        mockMvc.perform(withCurrentUser(
                post("/api/v1/delivery-managers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ))
            .andExpect(status().isCreated())
            .andDo(document("delivery-managers/create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("userId").type(JsonFieldType.STRING).description("사용자 ID"),
                    fieldWithPath("hubId").type(JsonFieldType.STRING).optional().description("소속 허브 ID"),
                    fieldWithPath("slackId").type(JsonFieldType.STRING).description("슬랙 ID"),
                    fieldWithPath("type").type(JsonFieldType.STRING)
                        .description("배송 담당자 타입(HUB_DELIVERY_MANAGER, COMPANY_DELIVERY_MANAGER)")
                ),
                commonDeliveryManagerResponseFields()
            ));
    }

    @Test
    @DisplayName("배송담당자 단건 조회 API 문서화")
    void getDeliveryManagerDocs() throws Exception {
        UUID deliveryManagerId = UUID.fromString("50000000-0000-0000-0000-000000000001");

        when(deliveryManagerService.getDeliveryManager(eq(deliveryManagerId), any()))
            .thenReturn(mockDeliveryManagerResponse());

        mockMvc.perform(withCurrentUser(
                get("/api/v1/delivery-managers/{deliveryManagerId}", deliveryManagerId)
            ))
            .andExpect(status().isOk())
            .andDo(document("delivery-managers/get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryManagerId").description("배송담당자 ID")
                ),
                commonDeliveryManagerResponseFields()
            ));
    }

    @Test
    @DisplayName("배송담당자 검색 API 문서화")
    void searchDeliveryManagersDocs() throws Exception {
        when(deliveryManagerService.searchDeliveryManagers(any(), any()))
            .thenReturn(mockDeliveryManagerPageResponse());

        mockMvc.perform(withCurrentUser(
                get("/api/v1/delivery-managers")
                    .param("hubId", "40000000-0000-0000-0000-000000000001")
                    .param("type", "COMPANY_DELIVERY_MANAGER")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sortBy", "createdAt")
                    .param("direction", "DESC")
            ))
            .andExpect(status().isOk())
            .andDo(document("delivery-managers/search",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("hubId").description("소속 허브 ID").optional(),
                    parameterWithName("type").description("배송 담당자 타입").optional(),
                    parameterWithName("page").description("페이지 번호").optional(),
                    parameterWithName("size").description("페이지 크기(10, 30, 50)").optional(),
                    parameterWithName("sortBy").description("정렬 기준(createdAt, updatedAt, deliverySequence)").optional(),
                    parameterWithName("direction").description("정렬 방향(ASC, DESC)").optional()
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                    fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("배송담당자 목록"),
                    fieldWithPath("data.content[].deliveryManagerId").type(JsonFieldType.STRING).description("배송담당자 ID"),
                    fieldWithPath("data.content[].hubId").type(JsonFieldType.STRING).optional().description("소속 허브 ID"),
                    fieldWithPath("data.content[].slackId").type(JsonFieldType.STRING).description("슬랙 ID"),
                    fieldWithPath("data.content[].type").type(JsonFieldType.STRING).description("배송 담당자 타입"),
                    fieldWithPath("data.content[].deliverySequence").type(JsonFieldType.NUMBER).description("배송 순번"),
                    fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시각"),
                    fieldWithPath("data.content[].updatedAt").type(JsonFieldType.NULL).optional().description("수정 시각"),
                    fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                    fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                    fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 데이터 수"),
                    fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                    fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @Test
    @DisplayName("배송담당자 수정 API 문서화")
    void updateDeliveryManagerDocs() throws Exception {
        UUID deliveryManagerId = UUID.fromString("50000000-0000-0000-0000-000000000001");

        UpdateDeliveryManagerRequest request = new UpdateDeliveryManagerRequest(
            UUID.fromString("40000000-0000-0000-0000-000000000002"),
            "U87654321",
            DeliveryManagerType.COMPANY_DELIVERY_MANAGER
        );

        when(deliveryManagerService.updateDeliveryManager(eq(deliveryManagerId), any(), any()))
            .thenReturn(mockDeliveryManagerResponse());

        mockMvc.perform(withCurrentUser(
                put("/api/v1/delivery-managers/{deliveryManagerId}", deliveryManagerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ))
            .andExpect(status().isOk())
            .andDo(document("delivery-managers/update",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryManagerId").description("배송담당자 ID")
                ),
                requestFields(
                    fieldWithPath("hubId").type(JsonFieldType.STRING).optional().description("소속 허브 ID"),
                    fieldWithPath("slackId").type(JsonFieldType.STRING).description("슬랙 ID"),
                    fieldWithPath("type").type(JsonFieldType.STRING).description("배송 담당자 타입")
                ),
                commonDeliveryManagerResponseFields()
            ));
    }

    @Test
    @DisplayName("배송담당자 삭제 API 문서화")
    void deleteDeliveryManagerDocs() throws Exception {
        UUID deliveryManagerId = UUID.fromString("50000000-0000-0000-0000-000000000001");

        doNothing().when(deliveryManagerService).deleteDeliveryManager(eq(deliveryManagerId), any());

        mockMvc.perform(withCurrentUser(
                delete("/api/v1/delivery-managers/{deliveryManagerId}", deliveryManagerId)
            ))
            .andExpect(status().isOk())
            .andDo(document("delivery-managers/delete",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryManagerId").description("배송담당자 ID")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    private org.springframework.restdocs.payload.ResponseFieldsSnippet commonDeliveryManagerResponseFields() {
        return responseFields(
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
            fieldWithPath("data.deliveryManagerId").type(JsonFieldType.STRING).description("배송담당자 ID"),
            fieldWithPath("data.hubId").type(JsonFieldType.STRING).optional().description("소속 허브 ID"),
            fieldWithPath("data.slackId").type(JsonFieldType.STRING).description("슬랙 ID"),
            fieldWithPath("data.type").type(JsonFieldType.STRING).description("배송 담당자 타입"),
            fieldWithPath("data.deliverySequence").type(JsonFieldType.NUMBER).description("배송 순번"),
            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시각"),
            fieldWithPath("data.updatedAt").type(JsonFieldType.NULL).optional().description("수정 시각"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
        );
    }
}
