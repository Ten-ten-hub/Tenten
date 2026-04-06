package com.team.product_service.product.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.common.exception.BusinessException;
import com.team.common.exception.GlobalExceptionHandler;
import com.team.product_service.global.config.AuditConfig;
import com.team.product_service.global.config.SecurityConfig;
import com.team.product_service.global.exception.ProductErrorCode;
import com.team.product_service.product.application.ProductService;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.domain.ProductStatus;
import com.team.product_service.product.presentation.dto.ProductCreateRequest;
import com.team.product_service.product.presentation.dto.ProductUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, AuditConfig.class, GlobalExceptionHandler.class})
class ProductControllerDocsTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    ProductService productService;

    private MockMvc mockMvc;

    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID HUB_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocs) {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .apply(documentationConfiguration(restDocs)
                .operationPreprocessors()
                .withRequestDefaults(prettyPrint())
                .withResponseDefaults(prettyPrint()))
            .build();
    }

    private ProductResult productResult() {
        return new ProductResult(
            PRODUCT_ID, COMPANY_ID, HUB_ID,
            "테스트 상품", ProductStatus.ON_SALE,
            new BigDecimal("10000.00"), "테스트 상품 설명입니다.",
            LocalDateTime.of(2024, 1, 1, 0, 0), USER_ID,
            LocalDateTime.of(2024, 1, 2, 0, 0), USER_ID
        );
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // POST /api/v1/products  —  상품 생성
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("상품 생성 - 성공")
    @WithMockUser(roles = "MASTER_ADMIN")
    void createProduct_success() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
            "테스트 상품", COMPANY_ID, HUB_ID, new BigDecimal("10000.00"), "테스트 상품 설명입니다."
        );
        given(productService.createProduct(any())).willReturn(productResult());

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/products")
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
            .andExpect(jsonPath("$.name").value("테스트 상품"))
            .andDo(document("product-create",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN, COMPANY_MANAGER)")
                ),
                requestFields(
                    fieldWithPath("name").type(JsonFieldType.STRING).description("상품명 (필수, 최대 150자)"),
                    fieldWithPath("companyId").type(JsonFieldType.STRING).description("업체 UUID (필수)"),
                    fieldWithPath("hubId").type(JsonFieldType.STRING).description("허브 UUID (필수)"),
                    fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).description("단가 (필수, 0 초과)"),
                    fieldWithPath("description").type(JsonFieldType.STRING).optional().description("상품 설명 (선택, 최대 500자)")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("companyId").type(JsonFieldType.STRING).description("업체 UUID"),
                    fieldWithPath("hubId").type(JsonFieldType.STRING).description("허브 UUID"),
                    fieldWithPath("name").type(JsonFieldType.STRING).description("상품명"),
                    fieldWithPath("status").type(JsonFieldType.STRING).description("상품 상태 (ON_SALE, OUT_OF_STOCK, DISCONTINUED)"),
                    fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).description("단가"),
                    fieldWithPath("description").type(JsonFieldType.STRING).optional().description("상품 설명"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).description("수정자 UUID")
                )
            ));
    }

    @Test
    @DisplayName("상품 생성 - 실패 (유효성 검증 - 상품명 공백)")
    @WithMockUser(roles = "MASTER_ADMIN")
    void createProduct_validationFail() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
            "", COMPANY_ID, HUB_ID, new BigDecimal("10000.00"), "설명"
        );

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/products")
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andDo(document("product-create-validation-fail",
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (INVALID_INPUT)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.ARRAY).description("필드별 유효성 오류 목록"),
                    fieldWithPath("details[].field").type(JsonFieldType.STRING).description("오류 필드명"),
                    fieldWithPath("details[].reason").type(JsonFieldType.STRING).description("오류 사유")
                )
            ));
    }

    @Test
    @DisplayName("상품 생성 - 실패 (중복 상품명 409)")
    @WithMockUser(roles = "COMPANY_MANAGER")
    void createProduct_duplicateName() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
            "중복 상품명", COMPANY_ID, HUB_ID, new BigDecimal("10000.00"), "설명"
        );
        given(productService.createProduct(any()))
            .willThrow(new BusinessException(ProductErrorCode.DUPLICATE_PRODUCT_NAME));

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/products")
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "COMPANY_MANAGER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_PRODUCT_NAME"))
            .andDo(document("product-create-duplicate",
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (DUPLICATE_PRODUCT_NAME)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GET /api/v1/products/{productId}  —  단건 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("상품 단건 조회 - 성공")
    void getProduct_success() throws Exception {
        given(productService.getProduct(PRODUCT_ID)).willReturn(productResult());

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/products/{productId}", PRODUCT_ID))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
            .andDo(document("product-get",
                pathParameters(
                    parameterWithName("productId").description("조회할 상품 UUID")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("companyId").type(JsonFieldType.STRING).description("업체 UUID"),
                    fieldWithPath("hubId").type(JsonFieldType.STRING).description("허브 UUID"),
                    fieldWithPath("name").type(JsonFieldType.STRING).description("상품명"),
                    fieldWithPath("status").type(JsonFieldType.STRING).description("상품 상태"),
                    fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).description("단가"),
                    fieldWithPath("description").type(JsonFieldType.STRING).optional().description("상품 설명"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).description("수정자 UUID")
                )
            ));
    }

    @Test
    @DisplayName("상품 단건 조회 - 실패 (존재하지 않는 상품 404)")
    void getProduct_notFound() throws Exception {
        given(productService.getProduct(PRODUCT_ID))
            .willThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/products/{productId}", PRODUCT_ID))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
            .andDo(document("product-get-not-found",
                pathParameters(
                    parameterWithName("productId").description("조회할 상품 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (PRODUCT_NOT_FOUND)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GET /api/v1/products  —  목록 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("상품 목록 조회 - 성공")
    void getProducts_success() throws Exception {
        PageImpl<ProductResult> page = new PageImpl<>(
            List.of(productResult()), PageRequest.of(0, 10), 1
        );
        given(productService.getProducts(any(), any())).willReturn(page);

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/products")
                .param("name", "테스트")
                .param("status", "ON_SALE")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andDo(document("product-list",
                queryParameters(
                    parameterWithName("name").optional().description("상품명 검색어 (부분 일치)"),
                    parameterWithName("companyId").optional().description("업체 UUID 필터"),
                    parameterWithName("hubId").optional().description("허브 UUID 필터"),
                    parameterWithName("status").optional().description("상품 상태 필터 (ON_SALE, OUT_OF_STOCK, DISCONTINUED)"),
                    parameterWithName("page").optional().description("페이지 번호 (0부터 시작, 기본값 0)"),
                    parameterWithName("size").optional().description("페이지 크기 (10, 30, 50, 기본값 10)")
                ),
                responseFields(
                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("상품 목록"),
                    fieldWithPath("content[].id").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("content[].companyId").type(JsonFieldType.STRING).description("업체 UUID"),
                    fieldWithPath("content[].hubId").type(JsonFieldType.STRING).description("허브 UUID"),
                    fieldWithPath("content[].name").type(JsonFieldType.STRING).description("상품명"),
                    fieldWithPath("content[].status").type(JsonFieldType.STRING).description("상품 상태"),
                    fieldWithPath("content[].unitPrice").type(JsonFieldType.NUMBER).description("단가"),
                    fieldWithPath("content[].description").type(JsonFieldType.STRING).optional().description("상품 설명"),
                    fieldWithPath("content[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("content[].createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("content[].updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                    fieldWithPath("content[].updatedBy").type(JsonFieldType.STRING).description("수정자 UUID"),
                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 상품 수"),
                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                    fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/products/{productId}  —  상품 수정
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("상품 수정 - 성공")
    @WithMockUser(roles = "HUB_ADMIN")
    void updateProduct_success() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
            "수정된 상품명", new BigDecimal("20000.00"), "수정된 설명", ProductStatus.OUT_OF_STOCK
        );
        ProductResult updated = new ProductResult(
            PRODUCT_ID, COMPANY_ID, HUB_ID,
            "수정된 상품명", ProductStatus.OUT_OF_STOCK,
            new BigDecimal("20000.00"), "수정된 설명",
            LocalDateTime.of(2024, 1, 1, 0, 0), USER_ID,
            LocalDateTime.of(2024, 1, 3, 0, 0), USER_ID
        );
        given(productService.updateProduct(any())).willReturn(updated);

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/products/{productId}", PRODUCT_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "HUB_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("수정된 상품명"))
            .andDo(document("product-update",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN, COMPANY_MANAGER)")
                ),
                pathParameters(
                    parameterWithName("productId").description("수정할 상품 UUID")
                ),
                requestFields(
                    fieldWithPath("name").type(JsonFieldType.STRING).optional().description("수정할 상품명 (최대 150자)"),
                    fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).optional().description("수정할 단가 (0 초과)"),
                    fieldWithPath("description").type(JsonFieldType.STRING).optional().description("수정할 상품 설명 (최대 500자)"),
                    fieldWithPath("status").type(JsonFieldType.STRING).optional().description("수정할 상품 상태 (ON_SALE, OUT_OF_STOCK, DISCONTINUED)")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("companyId").type(JsonFieldType.STRING).description("업체 UUID"),
                    fieldWithPath("hubId").type(JsonFieldType.STRING).description("허브 UUID"),
                    fieldWithPath("name").type(JsonFieldType.STRING).description("상품명"),
                    fieldWithPath("status").type(JsonFieldType.STRING).description("상품 상태"),
                    fieldWithPath("unitPrice").type(JsonFieldType.NUMBER).description("단가"),
                    fieldWithPath("description").type(JsonFieldType.STRING).optional().description("상품 설명"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).description("수정자 UUID")
                )
            ));
    }

    @Test
    @DisplayName("상품 수정 - 실패 (존재하지 않는 상품 404)")
    @WithMockUser(roles = "MASTER_ADMIN")
    void updateProduct_notFound() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest("수정 시도", null, null, null);
        given(productService.updateProduct(any()))
            .willThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/products/{productId}", PRODUCT_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
            .andDo(document("product-update-not-found",
                pathParameters(
                    parameterWithName("productId").description("수정할 상품 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (PRODUCT_NOT_FOUND)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/products/{productId}  —  상품 삭제
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("상품 삭제 - 성공")
    @WithMockUser(roles = "MASTER_ADMIN")
    void deleteProduct_success() throws Exception {
        willDoNothing().given(productService).deleteProduct(any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders
                .delete("/api/v1/products/{productId}", PRODUCT_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("product-delete",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN)")
                ),
                pathParameters(
                    parameterWithName("productId").description("삭제할 상품 UUID")
                )
            ));
    }

    @Test
    @DisplayName("상품 삭제 - 실패 (존재하지 않는 상품 404)")
    @WithMockUser(roles = "MASTER_ADMIN")
    void deleteProduct_notFound() throws Exception {
        willThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND))
            .given(productService).deleteProduct(any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders
                .delete("/api/v1/products/{productId}", PRODUCT_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
            .andDo(document("product-delete-not-found",
                pathParameters(
                    parameterWithName("productId").description("삭제할 상품 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (PRODUCT_NOT_FOUND)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }
}
