package com.team.companyservice.presentation.company;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.common.exception.GlobalExceptionHandler;
import com.team.companyservice.company.application.dto.response.CompanyPageResponse;
import com.team.companyservice.company.application.dto.response.CompanyResponse;
import com.team.companyservice.company.application.service.CompanyService;
import com.team.companyservice.company.domain.CompanyType;
import com.team.companyservice.company.presentation.ExternalCompanyController;
import com.team.companyservice.global.common.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
class ExternalCompanyControllerRestDocsTest {

    private MockMvc mockMvc;

    @Mock
    private CompanyService companyService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        ExternalCompanyController controller = new ExternalCompanyController(companyService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new CurrentUserTestArgumentResolver())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .apply(documentationConfiguration(restDocumentation))
            .build();
    }

    // 테스트에서 CurrentUser를 직접 주입한다.
    static class CurrentUserTestArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
        ) {
            String userId = webRequest.getHeader("X-User-Id");
            String role = webRequest.getHeader("X-User-Role");
            String hubId = webRequest.getHeader("X-Hub-Id");
            String companyId = webRequest.getHeader("X-Company-Id");

            return new CurrentUser(
                userId != null ? UUID.fromString(userId) : UUID.randomUUID(),
                role != null ? role : "MASTER_ADMIN",
                hubId != null && !hubId.isBlank() ? UUID.fromString(hubId) : null,
                companyId != null && !companyId.isBlank() ? UUID.fromString(companyId) : null
            );
        }
    }

    @DisplayName("업체 생성 문서화")
    @Test
    void createCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CompanyResponse response = CompanyResponse.builder()
            .id(companyId)
            .name("서울 생산업체")
            .companyType(CompanyType.PRODUCER)
            .hubId(hubId)
            .address("서울특별시 강남구 테헤란로 123")
            .addressDetail("5층")
            .zipcode("06234")
            .contactName("홍길동")
            .contactPhone("010-1234-5678")
            .contactSlackId("U123456")
            .isActive(true)
            .createdAt(LocalDateTime.of(2026, 3, 30, 10, 0))
            .updatedAt(null)
            .build();

        given(companyService.create(any(), any())).willReturn(response);

        String requestBody = """
                {
                  "name": "서울 생산업체",
                  "companyType": "PRODUCER",
                  "hubId": "%s",
                  "address": "서울특별시 강남구 테헤란로 123",
                  "addressDetail": "5층",
                  "zipcode": "06234",
                  "contactName": "홍길동",
                  "contactPhone": "010-1234-5678",
                  "contactSlackId": "U123456"
                }
                """.formatted(hubId);

        mockMvc.perform(post("/api/v1/companies")
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andDo(document("companies/create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName("X-User-Id").description("요청 사용자 ID"),
                    headerWithName("X-User-Role").description("요청 사용자 권한")
                ),
                requestFields(
                    fieldWithPath("name").type(JsonFieldType.STRING).description("업체명"),
                    fieldWithPath("companyType").type(JsonFieldType.STRING).description("업체 타입 (PRODUCER, RECEIVER)"),
                    fieldWithPath("hubId").type(JsonFieldType.STRING).description("관리 허브 ID"),
                    fieldWithPath("address").type(JsonFieldType.STRING).description("업체 주소"),
                    fieldWithPath("addressDetail").type(JsonFieldType.STRING).optional().description("상세 주소"),
                    fieldWithPath("zipcode").type(JsonFieldType.STRING).optional().description("우편번호"),
                    fieldWithPath("contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                    fieldWithPath("contactPhone").type(JsonFieldType.STRING).optional().description("담당자 연락처"),
                    fieldWithPath("contactSlackId").type(JsonFieldType.STRING).optional().description("담당자 슬랙 ID")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data.id").type(JsonFieldType.STRING).description("업체 ID"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING).description("업체명"),
                    fieldWithPath("data.companyType").type(JsonFieldType.STRING).description("업체 타입"),
                    fieldWithPath("data.hubId").type(JsonFieldType.STRING).description("관리 허브 ID"),
                    fieldWithPath("data.address").type(JsonFieldType.STRING).description("업체 주소"),
                    fieldWithPath("data.addressDetail").type(JsonFieldType.STRING).optional().description("상세 주소"),
                    fieldWithPath("data.zipcode").type(JsonFieldType.STRING).optional().description("우편번호"),
                    fieldWithPath("data.contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                    fieldWithPath("data.contactPhone").type(JsonFieldType.STRING).optional().description("담당자 연락처"),
                    fieldWithPath("data.contactSlackId").type(JsonFieldType.STRING).optional().description("담당자 슬랙 ID"),
                    fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                    fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
                    fieldWithPath("data.updatedAt").type(JsonFieldType.NULL).optional().description("수정일시"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @DisplayName("업체 단건 조회 문서화")
    @Test
    void getCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CompanyResponse response = CompanyResponse.builder()
            .id(companyId)
            .name("서울 생산업체")
            .companyType(CompanyType.PRODUCER)
            .hubId(hubId)
            .address("서울특별시 강남구 테헤란로 123")
            .addressDetail("5층")
            .zipcode("06234")
            .contactName("홍길동")
            .contactPhone("010-1234-5678")
            .contactSlackId("U123456")
            .isActive(true)
            .createdAt(LocalDateTime.of(2026, 3, 30, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 30, 11, 0))
            .build();

        given(companyService.get(companyId)).willReturn(response);

        mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
            .andExpect(status().isOk())
            .andDo(document("companies/get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("companyId").description("업체 ID")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data.id").type(JsonFieldType.STRING).description("업체 ID"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING).description("업체명"),
                    fieldWithPath("data.companyType").type(JsonFieldType.STRING).description("업체 타입"),
                    fieldWithPath("data.hubId").type(JsonFieldType.STRING).description("관리 허브 ID"),
                    fieldWithPath("data.address").type(JsonFieldType.STRING).description("업체 주소"),
                    fieldWithPath("data.addressDetail").type(JsonFieldType.STRING).optional().description("상세 주소"),
                    fieldWithPath("data.zipcode").type(JsonFieldType.STRING).optional().description("우편번호"),
                    fieldWithPath("data.contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                    fieldWithPath("data.contactPhone").type(JsonFieldType.STRING).optional().description("담당자 연락처"),
                    fieldWithPath("data.contactSlackId").type(JsonFieldType.STRING).optional().description("담당자 슬랙 ID"),
                    fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                    fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
                    fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정일시"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @DisplayName("업체 검색 문서화")
    @Test
    void searchCompanies() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CompanyResponse company = CompanyResponse.builder()
            .id(companyId)
            .name("서울 생산업체")
            .companyType(CompanyType.PRODUCER)
            .hubId(hubId)
            .address("서울특별시 강남구 테헤란로 123")
            .addressDetail("5층")
            .zipcode("06234")
            .contactName("홍길동")
            .contactPhone("010-1234-5678")
            .contactSlackId("U123456")
            .isActive(true)
            .createdAt(LocalDateTime.of(2026, 3, 30, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 30, 11, 0))
            .build();

        CompanyPageResponse response = new CompanyPageResponse(
            List.of(company),
            0,
            10,
            1,
            1,
            true,
            true
        );

        given(companyService.search(any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
            .willReturn(response);

        mockMvc.perform(get("/api/v1/companies")
                .param("keyword", "서울")
                .param("companyType", "PRODUCER")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("direction", "DESC"))
            .andExpect(status().isOk())
            .andDo(document("companies/search",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("keyword").optional().description("검색어"),
                    parameterWithName("companyType").optional().description("업체 타입"),
                    parameterWithName("hubId").optional().description("허브 ID"),
                    parameterWithName("isActive").optional().description("활성 여부"),
                    parameterWithName("sortBy").optional().description("정렬 기준(createdAt, updatedAt)"),
                    parameterWithName("direction").optional().description("정렬 방향(ASC, DESC)"),
                    parameterWithName("page").optional().description("페이지 번호"),
                    parameterWithName("size").optional().description("페이지 크기(10, 30, 50)")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data.content[].id").type(JsonFieldType.STRING).description("업체 ID"),
                    fieldWithPath("data.content[].name").type(JsonFieldType.STRING).description("업체명"),
                    fieldWithPath("data.content[].companyType").type(JsonFieldType.STRING).description("업체 타입"),
                    fieldWithPath("data.content[].hubId").type(JsonFieldType.STRING).description("허브 ID"),
                    fieldWithPath("data.content[].address").type(JsonFieldType.STRING).description("주소"),
                    fieldWithPath("data.content[].addressDetail").type(JsonFieldType.STRING).optional().description("상세 주소"),
                    fieldWithPath("data.content[].zipcode").type(JsonFieldType.STRING).optional().description("우편번호"),
                    fieldWithPath("data.content[].contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                    fieldWithPath("data.content[].contactPhone").type(JsonFieldType.STRING).optional().description("담당자 연락처"),
                    fieldWithPath("data.content[].contactSlackId").type(JsonFieldType.STRING).optional().description("담당자 슬랙 ID"),
                    fieldWithPath("data.content[].isActive").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                    fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
                    fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING).description("수정일시"),
                    fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                    fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                    fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 데이터 수"),
                    fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                    fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                    fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @DisplayName("업체 수정 문서화")
    @Test
    void updateCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        CompanyResponse response = CompanyResponse.builder()
            .id(companyId)
            .name("수정된 업체")
            .companyType(CompanyType.RECEIVER)
            .hubId(hubId)
            .address("서울특별시 송파구 올림픽로 77")
            .addressDetail("7층")
            .zipcode("05510")
            .contactName("김철수")
            .contactPhone("010-9999-8888")
            .contactSlackId("U999999")
            .isActive(true)
            .createdAt(LocalDateTime.of(2026, 3, 30, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 30, 12, 0))
            .build();

        given(companyService.update(eq(companyId), any(), any())).willReturn(response);

        String requestBody = """
                {
                  "name": "수정된 업체",
                  "companyType": "RECEIVER",
                  "hubId": "%s",
                  "address": "서울특별시 송파구 올림픽로 77",
                  "addressDetail": "7층",
                  "zipcode": "05510",
                  "contactName": "김철수",
                  "contactPhone": "010-9999-8888",
                  "contactSlackId": "U999999"
                }
                """.formatted(hubId);

        mockMvc.perform(put("/api/v1/companies/{companyId}", companyId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andDo(document("companies/update",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("companyId").description("업체 ID")
                ),
                requestHeaders(
                    headerWithName("X-User-Id").description("요청 사용자 ID"),
                    headerWithName("X-User-Role").description("요청 사용자 권한")
                ),
                requestFields(
                    fieldWithPath("name").type(JsonFieldType.STRING).description("업체명"),
                    fieldWithPath("companyType").type(JsonFieldType.STRING).description("업체 타입 (PRODUCER, RECEIVER)"),
                    fieldWithPath("hubId").type(JsonFieldType.STRING).description("관리 허브 ID"),
                    fieldWithPath("address").type(JsonFieldType.STRING).description("업체 주소"),
                    fieldWithPath("addressDetail").type(JsonFieldType.STRING).optional().description("상세 주소"),
                    fieldWithPath("zipcode").type(JsonFieldType.STRING).optional().description("우편번호"),
                    fieldWithPath("contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                    fieldWithPath("contactPhone").type(JsonFieldType.STRING).optional().description("담당자 연락처"),
                    fieldWithPath("contactSlackId").type(JsonFieldType.STRING).optional().description("담당자 슬랙 ID")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data.id").type(JsonFieldType.STRING).description("업체 ID"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING).description("업체명"),
                    fieldWithPath("data.companyType").type(JsonFieldType.STRING).description("업체 타입"),
                    fieldWithPath("data.hubId").type(JsonFieldType.STRING).description("관리 허브 ID"),
                    fieldWithPath("data.address").type(JsonFieldType.STRING).description("업체 주소"),
                    fieldWithPath("data.addressDetail").type(JsonFieldType.STRING).optional().description("상세 주소"),
                    fieldWithPath("data.zipcode").type(JsonFieldType.STRING).optional().description("우편번호"),
                    fieldWithPath("data.contactName").type(JsonFieldType.STRING).optional().description("담당자명"),
                    fieldWithPath("data.contactPhone").type(JsonFieldType.STRING).optional().description("담당자 연락처"),
                    fieldWithPath("data.contactSlackId").type(JsonFieldType.STRING).optional().description("담당자 슬랙 ID"),
                    fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                    fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
                    fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정일시"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @DisplayName("업체 삭제 문서화")
    @Test
    void deleteCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        doNothing().when(companyService).delete(eq(companyId), any());

        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andExpect(status().isOk())
            .andDo(document("companies/delete",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("companyId").description("업체 ID")
                ),
                requestHeaders(
                    headerWithName("X-User-Id").description("요청 사용자 ID"),
                    headerWithName("X-User-Role").description("요청 사용자 권한")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                )
            ));
    }

    @DisplayName("업체 관리자 지정 문서화")
    @Test
    void assignCompanyManager() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doNothing().when(companyService).assignManager(eq(companyId), eq(userId), any());

        String requestBody = """
                {
                  "userId": "%s"
                }
                """.formatted(userId);

        mockMvc.perform(patch("/api/v1/companies/{companyId}/manager", companyId)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andDo(document("companies/assign-manager",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("companyId").description("업체 ID")
                ),
                requestHeaders(
                    headerWithName("X-User-Id").description("요청 사용자 ID"),
                    headerWithName("X-User-Role").description("요청 사용자 권한")
                ),
                requestFields(
                    fieldWithPath("userId").type(JsonFieldType.STRING).description("업체 관리자로 지정할 사용자 ID")
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
