package com.team.hubservice.hubroute.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.hubservice.global.security.HubSecurityConfig;
import com.team.hubservice.hubroute.application.dto.route.HubRouteCreateCommand;
import com.team.hubservice.hubroute.application.dto.route.HubRouteResult;
import com.team.hubservice.hubroute.application.service.HubRouteService;
import com.team.hubservice.hubroute.application.dto.route.HubRouteUpdateCommand;
import com.team.hubservice.hubroute.presentation.controller.HubRouteController;
import com.team.hubservice.hubroute.presentation.dto.route.HubRouteCreateRequest;
import com.team.hubservice.hubroute.presentation.dto.route.HubRouteUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HubRouteController.class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
@Import(HubSecurityConfig.class)
class HubRouteControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HubRouteService hubRouteService;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("허브 이동 경로 생성 API 테스트")
    @WithMockUser(roles = "MASTER_ADMIN")
    void createHubRoute() throws Exception {
        UUID departureId = UUID.randomUUID();
        UUID arrivalId = UUID.randomUUID();
        HubRouteCreateRequest request = new HubRouteCreateRequest(departureId, arrivalId, 120, (Double) 150.5);
        HubRouteResult result = new HubRouteResult(UUID.randomUUID(), departureId, arrivalId, 120, (Double) 150.5, LocalDateTime.now(), UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID());

        Mockito.when(hubRouteService.createHubRoute(any(HubRouteCreateCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/hub-routes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andDo(document("hub-route-create",
                requestFields(
                    fieldWithPath("departureHubId").description("출발 허브 ID"),
                    fieldWithPath("arrivalHubId").description("도착 허브 ID"),
                    fieldWithPath("duration").description("소요 시간"),
                    fieldWithPath("distance").description("이동 거리")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.id").description("경로 ID"),
                    fieldWithPath("data.departureHubId").description("출발 허브 ID"),
                    fieldWithPath("data.arrivalHubId").description("도착 허브 ID"),
                    fieldWithPath("data.duration").description("소요 시간"),
                    fieldWithPath("data.distance").description("이동 거리"),
                    fieldWithPath("data.createdAt").description("생성 일시"),
                    fieldWithPath("data.createdBy").description("생성자 ID"),
                    fieldWithPath("data.updatedAt").description("수정 일시"),
                    fieldWithPath("data.updatedBy").description("수정자 ID")
                )
            ));
    }

    @Test
    @DisplayName("허브 이동 경로 수정 API 테스트")
    @WithMockUser(roles = "MASTER_ADMIN")
    void updateHubRoute() throws Exception {
        UUID routeId = UUID.randomUUID();
        HubRouteUpdateRequest request = new HubRouteUpdateRequest(100, 145.0);
        HubRouteResult result = new HubRouteResult(routeId, UUID.randomUUID(), UUID.randomUUID(), 100, 145.0, LocalDateTime.now(), UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID());

        Mockito.when(hubRouteService.updateHubRoute(eq(routeId), any(HubRouteUpdateCommand.class))).thenReturn(result);

        mockMvc.perform(patch("/api/v1/hub-routes/{routeId}", routeId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(document("hub-route-update",
                pathParameters(parameterWithName("routeId").description("수정할 경로 ID")),
                requestFields(
                    fieldWithPath("duration").description("수정할 소요 시간").optional(),
                    fieldWithPath("distance").description("수정할 이동 거리").optional()
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.id").description("경로 ID"),
                    fieldWithPath("data.departureHubId").description("출발 허브 ID"),
                    fieldWithPath("data.arrivalHubId").description("도착 허브 ID"),
                    fieldWithPath("data.duration").description("소요 시간"),
                    fieldWithPath("data.distance").description("이동 거리"),
                    fieldWithPath("data.createdAt").description("생성 일시"),
                    fieldWithPath("data.createdBy").description("생성자 ID"),
                    fieldWithPath("data.updatedAt").description("수정 일시"),
                    fieldWithPath("data.updatedBy").description("수정자 ID")
                )
            ));
    }

    @Test
    @DisplayName("허브 이동 경로 단건 조회 API 테스트")
    @WithMockUser(roles = "MASTER_ADMIN")
    void getHubRoute() throws Exception {
        UUID routeId = UUID.randomUUID();
        UUID departureId = UUID.randomUUID();
        UUID arrivalId = UUID.randomUUID();

        HubRouteResult result = new HubRouteResult(
            routeId, departureId, arrivalId, 120, 150.5,
            LocalDateTime.now(), UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID()
        );

        Mockito.when(hubRouteService.getHubRoute(routeId)).thenReturn(result);

        mockMvc.perform(get("/api/v1/hub-routes/{routeId}", routeId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("hub-route-get",
                pathParameters(
                    parameterWithName("routeId").description("조회할 경로 ID")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.id").description("경로 ID"),
                    fieldWithPath("data.departureHubId").description("출발 허브 ID"),
                    fieldWithPath("data.arrivalHubId").description("도착 허브 ID"),
                    fieldWithPath("data.duration").description("소요 시간"),
                    fieldWithPath("data.distance").description("이동 거리"),
                    fieldWithPath("data.createdAt").description("생성 일시"),
                    fieldWithPath("data.createdBy").description("생성자 ID"),
                    fieldWithPath("data.updatedAt").description("수정 일시"),
                    fieldWithPath("data.updatedBy").description("수정자 ID")
                )
            ));
    }

    @Test
    @DisplayName("허브 이동 경로 목록 조회 API 테스트")
    @WithMockUser(roles = "MASTER_ADMIN")
    void getHubRoutes() throws Exception {
        UUID departureId = UUID.randomUUID();
        UUID arrivalId = UUID.randomUUID();

        HubRouteResult result = new HubRouteResult(
            UUID.randomUUID(), departureId, arrivalId, 120, 150.5,
            LocalDateTime.now(), UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID()
        );

        PageImpl<HubRouteResult> pageResult = new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1);

        // 조건 없이 전체 조회하는 상황 모킹
        Mockito.when(hubRouteService.getHubRoutes(any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/hub-routes")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("hub-route-get-list",
                queryParameters(
                    parameterWithName("departureHubId").description("출발 허브 ID 조건 (선택)").optional(),
                    parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                    parameterWithName("size").description("페이지 크기 (10, 30, 50 중 택1)").optional()
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.content[].id").description("경로 ID"),
                    fieldWithPath("data.content[].departureHubId").description("출발 허브 ID"),
                    fieldWithPath("data.content[].arrivalHubId").description("도착 허브 ID"),
                    fieldWithPath("data.content[].duration").description("소요 시간"),
                    fieldWithPath("data.content[].distance").description("이동 거리"),
                    fieldWithPath("data.content[].createdAt").description("생성 일시").optional(),
                    fieldWithPath("data.content[].createdBy").description("생성자 ID").optional(),
                    fieldWithPath("data.content[].updatedAt").description("수정 일시").optional(),
                    fieldWithPath("data.content[].updatedBy").description("수정자 ID").optional(),
                    fieldWithPath("data.pageInfo.currentPage").description("현재 페이지 번호"),
                    fieldWithPath("data.pageInfo.size").description("페이지 당 데이터 개수"),
                    fieldWithPath("data.pageInfo.totalElements").description("전체 데이터 개수"),
                    fieldWithPath("data.pageInfo.totalPages").description("전체 페이지 수")
                )
            ));
    }

    @Test
    @DisplayName("허브 이동 경로 삭제 API 테스트")
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "MASTER_ADMIN")
    void deleteHubRoute() throws Exception {
        UUID routeId = UUID.randomUUID();

        // 삭제는 반환값이 없으므로 doNothing() 처리
        Mockito.doNothing().when(hubRouteService).deleteHubRoute(eq(routeId), any(UUID.class));

        mockMvc.perform(delete("/api/v1/hub-routes/{routeId}", routeId)
                .with(csrf()))
            .andExpect(status().isOk())
            .andDo(document("hub-route-delete",
                pathParameters(
                    parameterWithName("routeId").description("삭제할 경로 ID")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data").description("응답 데이터 (null이 정상)")
                )
            ));
    }
}
