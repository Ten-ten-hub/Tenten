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
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.deliveryservice.application.delivery.CreateDeliveryRequest;
import com.team.deliveryservice.application.delivery.DeliveryPageResponse;
import com.team.deliveryservice.application.delivery.DeliveryResponse;
import com.team.deliveryservice.application.delivery.DeliveryRouteLogResponse;
import com.team.deliveryservice.application.delivery.DeliveryService;
import com.team.deliveryservice.application.delivery.UpdateDeliveryRequest;
import com.team.deliveryservice.domain.delivery.DeliveryRouteStatus;
import com.team.deliveryservice.domain.delivery.DeliveryStatus;
import com.team.deliveryservice.presentation.common.CurrentUserArgumentResolver;
import com.team.deliveryservice.presentation.common.CurrentUserResolverConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(DeliveryController.class)
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
class DeliveryControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryService deliveryService;

    /**
     * 테스트용 CurrentUser 헤더를 공통으로 넣기 위한 헬퍼 메서드
     */
    private MockHttpServletRequestBuilder withCurrentUser(MockHttpServletRequestBuilder builder) {
        return builder
            .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
            .header("X-Role", "MASTER_ADMIN")
            .header("X-Hub-Id", "00000000-0000-0000-0000-000000000002")
            .header("X-Company-Id", "00000000-0000-0000-0000-000000000003");
    }

    /**
     * 공통 응답용 배송 경로 로그
     */
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

    /**
     * 공통 응답용 배송 정보
     */
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
    @DisplayName("배송 생성 API 문서화")
    void createDeliveryDocs() throws Exception {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            UUID.fromString("40000000-0000-0000-0000-000000000001"),
            UUID.fromString("40000000-0000-0000-0000-000000000002"),
            UUID.fromString("60000000-0000-0000-0000-000000000001"),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            UUID.fromString("50000000-0000-0000-0000-000000000001"),
            LocalDateTime.of(2026, 4, 1, 18, 0)
        );

        when(deliveryService.createDelivery(any(), any())).thenReturn(mockDeliveryResponse());

        mockMvc.perform(withCurrentUser(
                post("/api/v1/deliveries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ))
            .andExpect(status().isCreated())
            .andDo(document("deliveries/create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("orderId").type(JsonFieldType.STRING).description("주문 ID"),
                    fieldWithPath("originHubId").type(JsonFieldType.STRING).description("출발 허브 ID"),
                    fieldWithPath("destinationHubId").type(JsonFieldType.STRING).description("도착 허브 ID"),
                    fieldWithPath("receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 ID"),
                    fieldWithPath("deliveryAddress").type(JsonFieldType.STRING).description("배송 주소"),
                    fieldWithPath("deliveryAddressDetail").type(JsonFieldType.STRING).optional().description("배송 상세 주소"),
                    fieldWithPath("recipientName").type(JsonFieldType.STRING).description("수령인 이름"),
                    fieldWithPath("recipientSlackId").type(JsonFieldType.STRING).description("수령인 슬랙 ID"),
                    fieldWithPath("companyDeliveryManagerId").type(JsonFieldType.STRING).optional().description("업체 배송 담당자 ID"),
                    fieldWithPath("finalDispatchDeadlineAt").type(JsonFieldType.STRING).optional().description("최종 출고 마감 시각")
                ),
                responseFields(
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
                    subsectionWithPath("data.routeLogs").description("배송 경로 로그 목록"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @Test
    @DisplayName("배송 단건 조회 API 문서화")
    void getDeliveryDocs() throws Exception {
        UUID deliveryId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        when(deliveryService.getDelivery(eq(deliveryId), any())).thenReturn(mockDeliveryResponse());

        mockMvc.perform(withCurrentUser(
                get("/api/v1/deliveries/{deliveryId}", deliveryId)
            ))
            .andExpect(status().isOk())
            .andDo(document("deliveries/get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryId").description("배송 ID")
                ),
                responseFields(
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
                    subsectionWithPath("data.routeLogs").description("배송 경로 로그 목록"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @Test
    @DisplayName("배송 검색 API 문서화")
    void searchDeliveriesDocs() throws Exception {
        DeliveryPageResponse pageResponse = DeliveryPageResponse.builder()
            .content(List.of(mockDeliveryResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .hasNext(false)
            .build();

        when(deliveryService.searchDeliveries(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(withCurrentUser(
                get("/api/v1/deliveries")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sortBy", "createdAt")
                    .param("direction", "DESC")
            ))
            .andExpect(status().isOk())
            .andDo(document("deliveries/search",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("page").description("페이지 번호").optional(),
                    parameterWithName("size").description("페이지 크기").optional(),
                    parameterWithName("sortBy").description("정렬 기준").optional(),
                    parameterWithName("direction").description("정렬 방향").optional()
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),

                    fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("배송 목록"),
                    fieldWithPath("data.content[].deliveryId").type(JsonFieldType.STRING).description("배송 ID"),
                    fieldWithPath("data.content[].orderId").type(JsonFieldType.STRING).description("주문 ID"),
                    fieldWithPath("data.content[].deliveryStatus").type(JsonFieldType.STRING).description("배송 상태"),
                    fieldWithPath("data.content[].originHubId").type(JsonFieldType.STRING).description("출발 허브 ID"),
                    fieldWithPath("data.content[].destinationHubId").type(JsonFieldType.STRING).description("도착 허브 ID"),
                    fieldWithPath("data.content[].receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 ID"),
                    fieldWithPath("data.content[].deliveryAddress").type(JsonFieldType.STRING).description("배송 주소"),
                    fieldWithPath("data.content[].deliveryAddressDetail").type(JsonFieldType.STRING).optional().description("배송 상세 주소"),
                    fieldWithPath("data.content[].recipientName").type(JsonFieldType.STRING).description("수령인 이름"),
                    fieldWithPath("data.content[].recipientSlackId").type(JsonFieldType.STRING).description("수령인 슬랙 ID"),
                    fieldWithPath("data.content[].companyDeliveryManagerId").type(JsonFieldType.STRING).optional().description("업체 배송 담당자 ID"),
                    fieldWithPath("data.content[].startedAt").type(JsonFieldType.NULL).optional().description("배송 시작 시각"),
                    fieldWithPath("data.content[].completedAt").type(JsonFieldType.NULL).optional().description("배송 완료 시각"),
                    fieldWithPath("data.content[].finalDispatchDeadlineAt").type(JsonFieldType.STRING).optional().description("최종 출고 마감 시각"),
                    fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성 시각"),
                    fieldWithPath("data.content[].updatedAt").type(JsonFieldType.NULL).optional().description("수정 시각"),

                    fieldWithPath("data.content[].routeLogs").type(JsonFieldType.ARRAY).description("배송 경로 로그 목록"),
                    fieldWithPath("data.content[].routeLogs[].routeLogId").type(JsonFieldType.STRING).description("배송 경로 로그 ID"),
                    fieldWithPath("data.content[].routeLogs[].sequenceNo").type(JsonFieldType.NUMBER).description("경로 순번"),
                    fieldWithPath("data.content[].routeLogs[].departureHubId").type(JsonFieldType.STRING).description("출발 허브 ID"),
                    fieldWithPath("data.content[].routeLogs[].arrivalHubId").type(JsonFieldType.STRING).description("도착 허브 ID"),
                    fieldWithPath("data.content[].routeLogs[].expectedDistanceKm").type(JsonFieldType.NUMBER).description("예상 거리(km)"),
                    fieldWithPath("data.content[].routeLogs[].expectedDurationMinutes").type(JsonFieldType.NUMBER).description("예상 소요 시간(분)"),
                    fieldWithPath("data.content[].routeLogs[].routeStatus").type(JsonFieldType.STRING).description("배송 경로 상태"),
                    fieldWithPath("data.content[].routeLogs[].deliveryManagerId").type(JsonFieldType.STRING).description("배송 담당자 ID"),
                    fieldWithPath("data.content[].routeLogs[].departedAt").type(JsonFieldType.NULL).optional().description("출발 시각"),
                    fieldWithPath("data.content[].routeLogs[].arrivedAt").type(JsonFieldType.NULL).optional().description("도착 시각"),

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
    @DisplayName("배송 수정 API 문서화")
    void updateDeliveryDocs() throws Exception {
        UUID deliveryId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        UpdateDeliveryRequest request = new UpdateDeliveryRequest(
            "서울시 송파구 올림픽로 35",
            "202호",
            "김민지",
            "U99999999",
            UUID.fromString("50000000-0000-0000-0000-000000000002")
        );

        when(deliveryService.updateDelivery(eq(deliveryId), any(), any())).thenReturn(mockDeliveryResponse());

        mockMvc.perform(withCurrentUser(
                put("/api/v1/deliveries/{deliveryId}", deliveryId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ))
            .andExpect(status().isOk())
            .andDo(document("deliveries/update",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("deliveryId").description("배송 ID")
                ),
                requestFields(
                    fieldWithPath("deliveryAddress").type(JsonFieldType.STRING).description("배송 주소"),
                    fieldWithPath("deliveryAddressDetail").type(JsonFieldType.STRING).optional().description("배송 상세 주소"),
                    fieldWithPath("recipientName").type(JsonFieldType.STRING).description("수령인 이름"),
                    fieldWithPath("recipientSlackId").type(JsonFieldType.STRING).description("수령인 슬랙 ID"),
                    fieldWithPath("companyDeliveryManagerId").type(JsonFieldType.STRING).optional().description("업체 배송 담당자 ID")
                ),
                responseFields(
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
                    subsectionWithPath("data.routeLogs").description("배송 경로 로그 목록"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @Test
    @DisplayName("배송 삭제 API 문서화")
    void deleteDeliveryDocs() throws Exception {
        UUID deliveryId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        doNothing().when(deliveryService).deleteDelivery(eq(deliveryId), any());

        mockMvc.perform(withCurrentUser(
                delete("/api/v1/deliveries/{deliveryId}", deliveryId)
            ))
            .andExpect(status().isOk())
            .andDo(document("deliveries/delete",
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
}
