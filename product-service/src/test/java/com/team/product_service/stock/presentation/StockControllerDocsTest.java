package com.team.product_service.stock.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.common.exception.BusinessException;
import com.team.common.exception.GlobalExceptionHandler;
import com.team.product_service.global.config.AuditConfig;
import com.team.product_service.global.config.SecurityConfig;
import com.team.product_service.global.exception.ProductErrorCode;
import com.team.product_service.stock.application.StockService;
import com.team.product_service.stock.application.dto.StockHistoryResult;
import com.team.product_service.stock.application.dto.StockResult;
import com.team.product_service.stock.domain.StockHistoryType;
import com.team.product_service.stock.domain.StockStatus;
import com.team.product_service.stock.presentation.dto.StockAdjustRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

@WebMvcTest(controllers = {StockController.class, StockHistoryController.class})
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, AuditConfig.class, GlobalExceptionHandler.class})
class StockControllerDocsTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    StockService stockService;

    private MockMvc mockMvc;

    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID STOCK_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
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

    private StockResult stockResult() {
        return new StockResult(
            STOCK_ID, PRODUCT_ID, 100, StockStatus.AVAILABLE,
            LocalDateTime.of(2024, 1, 1, 0, 0), USER_ID,
            LocalDateTime.of(2024, 1, 2, 0, 0), USER_ID
        );
    }

    private StockHistoryResult stockHistoryResult() {
        return new StockHistoryResult(
            UUID.fromString("00000000-0000-0000-0000-000000000010"),
            STOCK_ID,
            StockHistoryType.ADJUSTMENT,
            100,
            100,
            ORDER_ID,
            LocalDateTime.of(2024, 1, 1, 0, 0),
            USER_ID
        );
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GET /api/v1/products/{productId}/stock  —  재고 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("재고 조회 - 성공")
    void getStock_success() throws Exception {
        given(stockService.getStock(PRODUCT_ID)).willReturn(stockResult());

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/products/{productId}/stock", PRODUCT_ID))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(STOCK_ID.toString()))
            .andDo(document("stock-get",
                pathParameters(
                    parameterWithName("productId").description("상품 UUID")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("재고 UUID"),
                    fieldWithPath("productId").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("quantity").type(JsonFieldType.NUMBER).description("재고 수량"),
                    fieldWithPath("status").type(JsonFieldType.STRING).description("재고 상태 (AVAILABLE, SHORTAGE, SOLD_OUT)"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 일시").optional(),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).description("수정자 UUID").optional()
                )
            ));
    }

    @Test
    @DisplayName("재고 조회 - 실패 (존재하지 않는 상품 404)")
    void getStock_notFound() throws Exception {
        given(stockService.getStock(PRODUCT_ID))
            .willThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/products/{productId}/stock", PRODUCT_ID))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
            .andDo(document("stock-get-not-found",
                pathParameters(
                    parameterWithName("productId").description("상품 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (PRODUCT_NOT_FOUND)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/products/{productId}/stock  —  재고 조정
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("재고 조정 - 성공")
    @WithMockUser(roles = "MASTER_ADMIN")
    void adjustStock_success() throws Exception {
        StockAdjustRequest request = new StockAdjustRequest(100);
        given(stockService.adjustStock(any())).willReturn(stockResult());

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(100))
            .andDo(document("stock-adjust",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN, COMPANY_MANAGER)")
                ),
                pathParameters(
                    parameterWithName("productId").description("상품 UUID")
                ),
                requestFields(
                    fieldWithPath("quantity").type(JsonFieldType.NUMBER).description("조정할 재고 수량 (0 이상)")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("재고 UUID"),
                    fieldWithPath("productId").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("quantity").type(JsonFieldType.NUMBER).description("재고 수량"),
                    fieldWithPath("status").type(JsonFieldType.STRING).description("재고 상태 (AVAILABLE, SHORTAGE, SOLD_OUT)"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 일시").optional(),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).description("수정자 UUID").optional()
                )
            ));
    }

    @Test
    @DisplayName("재고 조정 - 실패 (유효성 검증 - 수량 음수)")
    @WithMockUser(roles = "MASTER_ADMIN")
    @SuppressWarnings("DataFlowIssue")
    void adjustStock_validationFail() throws Exception {
        StockAdjustRequest request = new StockAdjustRequest(-1);

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andDo(document("stock-adjust-validation-fail",
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
    @DisplayName("재고 조정 - 실패 (재고 부족 400)")
    @WithMockUser(roles = "MASTER_ADMIN")
    void adjustStock_belowZero() throws Exception {
        StockAdjustRequest request = new StockAdjustRequest(0);
        given(stockService.adjustStock(any()))
            .willThrow(new BusinessException(ProductErrorCode.STOCK_BELOW_ZERO));

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("STOCK_BELOW_ZERO"))
            .andDo(document("stock-adjust-below-zero",
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (STOCK_BELOW_ZERO)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GET /api/v1/stocks/histories  —  재고 이력 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("재고 이력 조회 - 성공")
    void getStockHistories_success() throws Exception {
        PageImpl<StockHistoryResult> page = new PageImpl<>(
            List.of(stockHistoryResult()), PageRequest.of(0, 10), 1
        );
        given(stockService.getStockHistories(any(), any())).willReturn(page);

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/stocks/histories")
                .param("productId", PRODUCT_ID.toString())
                .param("type", "ADJUSTMENT")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andDo(document("stock-history-list",
                queryParameters(
                    parameterWithName("stockId").optional().description("재고 UUID 필터"),
                    parameterWithName("productId").optional().description("상품 UUID 필터"),
                    parameterWithName("orderId").optional().description("주문 UUID 필터"),
                    parameterWithName("type").optional().description("이력 타입 필터 (INBOUND, OUTBOUND, RESTORE, ADJUSTMENT)"),
                    parameterWithName("page").optional().description("페이지 번호 (0부터 시작, 기본값 0)"),
                    parameterWithName("size").optional().description("페이지 크기 (10, 30, 50, 기본값 10)")
                ),
                responseFields(
                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("재고 이력 목록"),
                    fieldWithPath("content[].id").type(JsonFieldType.STRING).description("이력 UUID"),
                    fieldWithPath("content[].stock_id").type(JsonFieldType.STRING).description("재고 UUID"),
                    fieldWithPath("content[].orderId").type(JsonFieldType.STRING).optional().description("주문 UUID (주문 관련 이력인 경우)"),
                    fieldWithPath("content[].type").type(JsonFieldType.STRING).description("이력 타입 (INBOUND, OUTBOUND, RESTORE, ADJUSTMENT)"),
                    fieldWithPath("content[].quantityChange").type(JsonFieldType.NUMBER).description("변경 수량"),
                    fieldWithPath("content[].quantityAfter").type(JsonFieldType.NUMBER).description("변경 후 수량"),
                    fieldWithPath("content[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("content[].createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 이력 수"),
                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                    fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기")
                )
            ));
    }
}
