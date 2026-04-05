package com.team.hubservice.hubroute.api;

import com.team.hubservice.global.security.HubSecurityConfig;
import com.team.hubservice.hubroute.application.HubRouteAiService;
import com.team.hubservice.hubroute.application.HubRouteOptimalService;
import com.team.hubservice.hubroute.application.OptimalRouteQuery;
import com.team.hubservice.hubroute.application.OptimalRouteResult;
import com.team.hubservice.hubroute.application.RoutePathInfo;
import com.team.hubservice.hubroute.presentation.HubRouteInternalController;
import com.team.hubservice.hubroute.presentation.dto.AiRouteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HubRouteInternalController.class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
@Import(HubSecurityConfig.class)
class HubRouteInternalControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HubRouteOptimalService hubRouteOptimalService;

    @MockitoBean
    private HubRouteAiService hubRouteAiService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("허브 최적 경로 조회 내부 API 테스트")
    void getOptimalRoute() throws Exception {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        UUID viaHubId = UUID.randomUUID();

        List<RoutePathInfo> pathList = List.of(
            new RoutePathInfo(1, departureHubId, viaHubId, 60, 35.5),
            new RoutePathInfo(2, viaHubId, arrivalHubId, 50, 42.0)
        );

        OptimalRouteResult result = new OptimalRouteResult(
            departureHubId, arrivalHubId, 110, 77.5, pathList
        );

        Mockito.when(hubRouteOptimalService.findOptimalRoute(any(OptimalRouteQuery.class))).thenReturn(result);

        mockMvc.perform(get("/internal/v1/hub-route/optimal")
                .header("X-Internal-Request", "true")
                .param("departureHubId", departureHubId.toString())
                .param("arrivalHubId", arrivalHubId.toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("hub-route-optimal-get",
                requestHeaders(
                    headerWithName("X-Internal-Request").description("내부망 접근 확인 헤더 (반드시 true여야 함)")
                ),
                queryParameters(
                    parameterWithName("departureHubId").description("출발 허브 ID"),
                    parameterWithName("arrivalHubId").description("도착 허브 ID")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.departureHubId").description("출발 허브 ID"),
                    fieldWithPath("data.arrivalHubId").description("도착 허브 ID"),
                    fieldWithPath("data.totalDuration").description("총 소요 시간"),
                    fieldWithPath("data.totalDistance").description("총 이동 거리"),
                    fieldWithPath("data.routePathList[].sequence").description("경로 순서"),
                    fieldWithPath("data.routePathList[].departureHubId").description("구간 출발 허브 ID"),
                    fieldWithPath("data.routePathList[].arrivalHubId").description("구간 도착 허브 ID"),
                    fieldWithPath("data.routePathList[].duration").description("구간 소요 시간"),
                    fieldWithPath("data.routePathList[].distance").description("구간 이동 거리")
                )
            ));
    }

    @Test
    @DisplayName("AI 학습용 단건 허브 경로 조회 내부 API 테스트")
    @WithMockUser
    void getRouteForAi() throws Exception {
        UUID originId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        AiRouteResponse aiResponse = new AiRouteResponse(
            180, 385.5, 37.5665, 126.9780, 35.1796, 129.0756
        );

        Mockito.when(hubRouteAiService.getRouteInfoForAi(originId, destinationId))
            .thenReturn(aiResponse);

        mockMvc.perform(get("/internal/v1/hub-route")
                .header("X-Internal-Request", "true")
                .param("originId", originId.toString())
                .param("destinationId", destinationId.toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("hub-route-ai-get",
                requestHeaders(
                    headerWithName("X-Internal-Request").description("내부망 접근 확인 헤더 (반드시 true여야 함)")
                ),
                queryParameters(
                    parameterWithName("originId").description("출발 허브 ID"),
                    parameterWithName("destinationId").description("도착 허브 ID")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.duration").description("소요 시간 (분)"),
                    fieldWithPath("data.distance").description("이동 거리 (km)"),
                    fieldWithPath("data.originLat").description("출발 허브 위도"),
                    fieldWithPath("data.originLng").description("출발 허브 경도"),
                    fieldWithPath("data.destLat").description("도착 허브 위도"),
                    fieldWithPath("data.destLng").description("도착 허브 경도")
                )
            ));
    }
}
