package com.team.hubservice.hub.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.hubservice.hub.application.HubCreateCommand;
import com.team.hubservice.hub.application.HubResult;
import com.team.hubservice.hub.application.HubService;
import com.team.hubservice.hub.presentation.dto.HubCreateRequest;
import com.team.hubservice.hub.presentation.HubController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HubController.class)
@AutoConfigureRestDocs
class HubControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HubService hubService;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("허브 생성 API 문서화 테스트")
    @WithMockUser(roles = "MASTER")
    void createHub() throws Exception {
        // Given
        HubCreateRequest request = new HubCreateRequest(
            "서울특별시 센터", "서울특별시 송파구 송파대로 55", 37.495, 127.122
        );

        HubResult result = new HubResult(
            UUID.randomUUID(), "서울특별시 센터", "서울특별시 송파구 송파대로 55", 37.495, 127.122,
            LocalDateTime.now(), UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID()
        );

        Mockito.when(hubService.createHub(any(HubCreateCommand.class))).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/v1/hubs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("서울특별시 센터"))
            .andDo(document("hub-create",
                requestFields(
                    fieldWithPath("name").description("허브 이름"),
                    fieldWithPath("address").description("허브 주소"),
                    fieldWithPath("latitude").description("위도"),
                    fieldWithPath("longitude").description("경도")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("data.id").description("허브 ID"),
                    fieldWithPath("data.name").description("허브 이름"),
                    fieldWithPath("data.address").description("허브 주소"),
                    fieldWithPath("data.latitude").description("위도"),
                    fieldWithPath("data.longitude").description("경도"),
                    fieldWithPath("data.createdAt").description("생성 일시"),
                    fieldWithPath("data.createdBy").description("생성자 ID"),
                    fieldWithPath("data.updatedAt").description("수정 일시"),
                    fieldWithPath("data.updatedBy").description("수정자 ID")
                )
            ));
    }
}
