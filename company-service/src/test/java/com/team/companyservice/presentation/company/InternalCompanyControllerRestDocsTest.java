package com.team.companyservice.presentation.company;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.companyservice.company.application.dto.response.CompanyInternalResponse;
import com.team.companyservice.company.application.service.CompanyService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = com.team.companyservice.company.presentation.InternalCompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureObservability
@TestPropertySource(properties = {
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.docker.compose.enabled=false",
    "management.tracing.enabled=false"
})
class InternalCompanyControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @Test
    @DisplayName("내부 업체 단건 조회 API 문서화")
    void getInternalCompanyDocs() throws Exception {
        UUID companyId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        CompanyInternalResponse response = new CompanyInternalResponse(
            companyId,
            "테스트 업체",
            "RECEIVER",
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            "서울시 강남구 테헤란로 123",
            "101호",
            "홍길동",
            "U12345678",
            true
        );

        when(companyService.getInternalCompany(eq(companyId))).thenReturn(response);

        mockMvc.perform(get("/internal/v1/companies/{companyId}", companyId))
            .andExpect(status().isOk())
            .andDo(document("internal-companies/get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("companyId").description("업체 ID")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                    fieldWithPath("data.id").type(JsonFieldType.STRING).description("업체 ID"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING).description("업체명"),
                    fieldWithPath("data.companyType").type(JsonFieldType.STRING).description("업체 타입"),
                    fieldWithPath("data.hubId").type(JsonFieldType.STRING).description("소속 허브 ID"),
                    fieldWithPath("data.address").type(JsonFieldType.STRING).description("업체 주소"),
                    fieldWithPath("data.addressDetail").type(JsonFieldType.STRING).description("업체 상세 주소"),
                    fieldWithPath("data.contactName").type(JsonFieldType.STRING).description("담당자 이름"),
                    fieldWithPath("data.contactSlackId").type(JsonFieldType.STRING).description("담당자 슬랙 ID"),
                    fieldWithPath("data.active").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }
}
