package com.team.product_service.stock.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.common.exception.GlobalExceptionHandler;
import com.team.product_service.global.config.AuditConfig;
import com.team.product_service.global.config.SecurityConfig;
import com.team.product_service.stock.application.StockService;
import com.team.product_service.stock.application.dto.StockResult;
import com.team.product_service.stock.domain.StockStatus;
import com.team.product_service.stock.presentation.dto.StockAdjustRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {StockController.class, StockHistoryController.class})
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, AuditConfig.class, GlobalExceptionHandler.class})
class StockControllerAuthTest {

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

    private String adjustRequestBody() throws Exception {
        return objectMapper.writeValueAsString(new StockAdjustRequest(100));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 재고 조정 권한 테스트
    // 허용: MASTER_ADMIN, HUB_ADMIN, COMPANY_MANAGER
    // 거부: HUB_DELIVERY_MANAGER, COM_DELIVERY_MANAGER
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("재고 조정 권한")
    class AdjustStockAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            given(stockService.adjustStock(any())).willReturn(stockResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "MASTER_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustRequestBody()))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HUB_ADMIN - 허용")
        void hubAdmin_allowed() throws Exception {
            given(stockService.adjustStock(any())).willReturn(stockResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustRequestBody()))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 허용")
        void companyManager_allowed() throws Exception {
            given(stockService.adjustStock(any())).willReturn(stockResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustRequestBody()))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HUB_DELIVERY_MANAGER - 거부 (403)")
        void hubDeliveryManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("stock-adjust-forbidden",
                    responseFields(
                        fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (FORBIDDEN)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                    )
                ));
        }

        @Test
        @DisplayName("COM_DELIVERY_MANAGER - 거부 (403)")
        void comDeliveryManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COM_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}/stock", PRODUCT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(adjustRequestBody()))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andDo(document("stock-adjust-unauthorized",
                    responseFields(
                        fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (UNAUTHORIZED)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                    )
                ));
        }
    }
}
