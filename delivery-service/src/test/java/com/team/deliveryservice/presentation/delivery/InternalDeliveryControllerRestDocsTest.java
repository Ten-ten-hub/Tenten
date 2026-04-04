package com.team.deliveryservice.presentation.delivery;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.deliveryservice.delivery.application.dto.request.ChangeDeliveryStatusRequest;
import com.team.deliveryservice.delivery.application.dto.request.CreateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryRouteLogResponse;
import com.team.deliveryservice.delivery.application.service.DeliveryService;
import com.team.deliveryservice.delivery.domain.DeliveryRouteStatus;
import com.team.deliveryservice.delivery.domain.DeliveryStatus;
import com.team.deliveryservice.delivery.presentation.InternalDeliveryController;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalDeliveryController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureObservability
@TestPropertySource(properties = {
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.docker.compose.enabled=false",
    "management.tracing.enabled=false"
})
class InternalDeliveryControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryService deliveryService;

    private DeliveryRouteLogResponse mockRouteLogResponse() {
        return DeliveryRouteLogResponse.builder()
            .routeLogId(UUID.fromString("30000000-0000-0000-0000-000000000001"))
            .sequenceNo(1)
            .departureHubId(UUID.fromString("40000000-0000-0000-0000-000000000001"))
            .arrivalHubId(UUID.fromString("40000000-0000-0000-0000-000000000002"))
            .expectedDistanceKm(new BigDecimal("12.50"))
            .expectedDurationMinutes(30)
            .routeStatus(DeliveryRouteStatus.WAITING_AT_HUB)
            .deliveryManagerId(UUID.fromString("50000000-0000-0000-0000-000000000001"))
            .departedAt(null)
            .arrivedAt(null)
            .build();
    }

    private DeliveryResponse mockDeliveryResponse() {
        return DeliveryResponse.builder()
            .deliveryId(UUID.fromString("10000000-0000-0000-0000-000000000001"))
            .orderId(UUID.fromString("20000000-0000-0000-0000-000000000001"))
            .deliveryStatus(DeliveryStatus.WAITING_AT_HUB)
            .originHubId(UUID.fromString("40000000-0000-0000-0000-000000000001"))
            .destinationHubId(UUID.fromString("40000000-0000-0000-0000-000000000002"))
            .receiverCompanyId(UUID.fromString("60000000-0000-0000-0000-000000000001"))
            .deliveryAddress("서울시 강남구 테헤란로 123")
            .deliveryAddressDetail("101호")
            .recipientName("홍길동")
            .recipientSlackId("U12345678")
            .companyDeliveryManagerId(UUID.fromString("50000000-0000-0000-0000-000000000001"))
            .startedAt(null)
            .completedAt(null)
            .finalDispatchDeadlineAt(LocalDateTime.of(2026, 4, 1, 18, 0))
            .createdAt(LocalDateTime.of(2026, 3, 31, 10, 0))
            .updatedAt(null)
            .routeLogs(List.of(mockRouteLogResponse()))
            .build();
    }

    @Test
    @DisplayName("내부 배송 생성 API 문서화")
    void createDeliveryDocs() throws Exception {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
            UUID.randomUUID(), // orderId
            UUID.randomUUID(), // orderedBy
            UUID.randomUUID(), // supplierCompanyId
            UUID.randomUUID(), // receiverCompanyId
            LocalDateTime.of(2026, 4, 1, 18, 0), // deadlineAt
            "문 앞에 놓아주세요" // requestNote
        );

        when(deliveryService.createDelivery(any())).thenReturn(mockDeliveryResponse());

        mockMvc.perform(
                post("/internal/v1/deliveries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated())
            .andDo(document("internal-deliveries/create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("orderId").type(JsonFieldType.STRING).description("주문 ID"),
                    fieldWithPath("orderedBy").type(JsonFieldType.STRING).description("주문자 ID"),
                    fieldWithPath("supplierCompanyId").type(JsonFieldType.STRING).description("공급 업체 ID"),
                    fieldWithPath("receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 ID"),
                    fieldWithPath("deadlineAt").type(JsonFieldType.STRING).description("납기 일시"),
                    fieldWithPath("requestNote").type(JsonFieldType.STRING).optional().description("요청사항")
                ),
                commonDeliveryResponseFields()
            ));
    }

    @Test
    @DisplayName("내부 배송 단건 조회 API 문서화")
    void getDeliveryDocs() throws Exception {
        UUID deliveryId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        when(deliveryService.getDelivery(eq(deliveryId))).thenReturn(mockDeliveryResponse());

        mockMvc.perform(get("/internal/v1/deliveries/{deliveryId}", deliveryId))
            .andExpect(status().isOk())
            .andDo(document("internal-deliveries/get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryId").description("배송 ID")
                ),
                commonDeliveryResponseFields()
            ));
    }

    @Test
    @DisplayName("내부 배송 상태 변경 API 문서화")
    void changeStatusDocs() throws Exception {
        UUID deliveryId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        ChangeDeliveryStatusRequest request = new ChangeDeliveryStatusRequest(DeliveryStatus.MOVING_BETWEEN_HUBS);

        when(deliveryService.changeDeliveryStatus(eq(deliveryId), any())).thenReturn(mockDeliveryResponse());

        mockMvc.perform(
                patch("/internal/v1/deliveries/{deliveryId}/status", deliveryId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andDo(document("internal-deliveries/change-status",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryId").description("배송 ID")
                ),
                requestFields(
                    fieldWithPath("deliveryStatus").type(JsonFieldType.STRING).description("변경할 배송 상태")
                ),
                commonDeliveryResponseFields()
            ));
    }

    @Test
    @DisplayName("내부 배송 취소 API 문서화")
    void cancelDeliveryDocs() throws Exception {
        UUID deliveryId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        when(deliveryService.cancelDelivery(eq(deliveryId))).thenReturn(mockDeliveryResponse());

        mockMvc.perform(patch("/internal/v1/deliveries/{deliveryId}/cancel", deliveryId))
            .andExpect(status().isOk())
            .andDo(document("internal-deliveries/cancel",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryId").description("배송 ID")
                ),
                commonDeliveryResponseFields()
            ));
    }

    @Test
    @DisplayName("내부 배송 삭제 API 문서화")
    void deleteDeliveryDocs() throws Exception {
        UUID deliveryId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        doNothing().when(deliveryService).deleteDelivery(eq(deliveryId));

        mockMvc.perform(delete("/internal/v1/deliveries/{deliveryId}", deliveryId))
            .andExpect(status().isOk())
            .andDo(document("internal-deliveries/delete",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryId").description("배송 ID")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    private ResponseFieldsSnippet commonDeliveryResponseFields() {
        return responseFields(
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
            fieldWithPath("data.deliveryId").type(JsonFieldType.STRING).description("배송 ID"),
            fieldWithPath("data.orderId").type(JsonFieldType.STRING).description("주문 ID"),
            fieldWithPath("data.deliveryStatus").type(JsonFieldType.STRING).description("배송 상태"),
            fieldWithPath("data.originHubId").type(JsonFieldType.STRING).description("출발 허브 ID"),
            fieldWithPath("data.destinationHubId").type(JsonFieldType.STRING).description("도착 허브 ID"),
            fieldWithPath("data.receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 ID"),
            fieldWithPath("data.deliveryAddress").type(JsonFieldType.STRING).description("배송 주소"),
            fieldWithPath("data.deliveryAddressDetail").type(JsonFieldType.STRING).optional().description("배송 상세 주소"),
            fieldWithPath("data.recipientName").type(JsonFieldType.STRING).description("수령인 이름"),
            fieldWithPath("data.recipientSlackId").type(JsonFieldType.STRING).description("수령인 슬랙 ID"),
            fieldWithPath("data.companyDeliveryManagerId").type(JsonFieldType.STRING).optional().description("업체 배송 담당자 ID"),
            fieldWithPath("data.startedAt").type(JsonFieldType.NULL).optional().description("배송 시작 시각"),
            fieldWithPath("data.completedAt").type(JsonFieldType.NULL).optional().description("배송 완료 시각"),
            fieldWithPath("data.finalDispatchDeadlineAt").type(JsonFieldType.STRING).optional().description("최종 출고 마감 시각"),
            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시각"),
            fieldWithPath("data.updatedAt").type(JsonFieldType.NULL).optional().description("수정 시각"),
            fieldWithPath("data.routeLogs").type(JsonFieldType.ARRAY).description("배송 경로 로그 목록"),
            fieldWithPath("data.routeLogs[].routeLogId").type(JsonFieldType.STRING).description("배송 경로 로그 ID"),
            fieldWithPath("data.routeLogs[].sequenceNo").type(JsonFieldType.NUMBER).description("경로 순번"),
            fieldWithPath("data.routeLogs[].departureHubId").type(JsonFieldType.STRING).description("출발 허브 ID"),
            fieldWithPath("data.routeLogs[].arrivalHubId").type(JsonFieldType.STRING).description("도착 허브 ID"),
            fieldWithPath("data.routeLogs[].expectedDistanceKm").type(JsonFieldType.NUMBER).description("예상 거리(km)"),
            fieldWithPath("data.routeLogs[].expectedDurationMinutes").type(JsonFieldType.NUMBER).description("예상 소요 시간(분)"),
            fieldWithPath("data.routeLogs[].routeStatus").type(JsonFieldType.STRING).description("배송 경로 상태"),
            fieldWithPath("data.routeLogs[].deliveryManagerId").type(JsonFieldType.STRING).optional().description("허브 배송 담당자 ID"),
            fieldWithPath("data.routeLogs[].departedAt").type(JsonFieldType.NULL).optional().description("출발 시각"),
            fieldWithPath("data.routeLogs[].arrivedAt").type(JsonFieldType.NULL).optional().description("도착 시각"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
        );
    }
}
