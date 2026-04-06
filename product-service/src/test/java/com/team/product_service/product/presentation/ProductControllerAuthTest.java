package com.team.product_service.product.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.common.exception.GlobalExceptionHandler;
import com.team.product_service.global.config.AuditConfig;
import com.team.product_service.global.config.SecurityConfig;
import com.team.product_service.product.application.ProductService;
import com.team.product_service.product.application.dto.ProductResult;
import com.team.product_service.product.domain.ProductStatus;
import com.team.product_service.product.presentation.dto.ProductCreateRequest;
import com.team.product_service.product.presentation.dto.ProductUpdateRequest;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, AuditConfig.class, GlobalExceptionHandler.class})
class ProductControllerAuthTest {

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

    private String createRequestBody() throws Exception {
        return objectMapper.writeValueAsString(
            new ProductCreateRequest("테스트 상품", COMPANY_ID, HUB_ID, new BigDecimal("10000.00"), "설명")
        );
    }

    private String updateRequestBody() throws Exception {
        return objectMapper.writeValueAsString(
            new ProductUpdateRequest("수정 상품명", null, null, null)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 상품 생성 권한 테스트
    // 허용: MASTER_ADMIN, HUB_ADMIN, COMPANY_MANAGER
    // 거부: HUB_DELIVERY_MANAGER, COM_DELIVERY_MANAGER
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("상품 생성 권한")
    class CreateProductAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            given(productService.createProduct(any())).willReturn(productResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/products")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "MASTER_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("HUB_ADMIN - 허용")
        void hubAdmin_allowed() throws Exception {
            given(productService.createProduct(any())).willReturn(productResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/products")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 허용")
        void companyManager_allowed() throws Exception {
            given(productService.createProduct(any())).willReturn(productResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/products")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("HUB_DELIVERY_MANAGER - 거부 (403)")
        void hubDeliveryManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/products")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("product-create-forbidden",
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
                    .post("/api/v1/products")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COM_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andDo(document("product-create-unauthorized",
                    responseFields(
                        fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (UNAUTHORIZED)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                    )
                ));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 상품 수정 권한 테스트
    // 허용: MASTER_ADMIN, HUB_ADMIN, COMPANY_MANAGER
    // 거부: HUB_DELIVERY_MANAGER, COM_DELIVERY_MANAGER
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("상품 수정 권한")
    class UpdateProductAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            given(productService.updateProduct(any())).willReturn(productResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "MASTER_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HUB_ADMIN - 허용")
        void hubAdmin_allowed() throws Exception {
            given(productService.updateProduct(any())).willReturn(productResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 허용")
        void companyManager_allowed() throws Exception {
            given(productService.updateProduct(any())).willReturn(productResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HUB_DELIVERY_MANAGER - 거부 (403)")
        void hubDeliveryManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("product-update-forbidden",
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
                    .patch("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COM_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/products/{productId}", PRODUCT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 상품 삭제 권한 테스트
    // 허용: MASTER_ADMIN, HUB_ADMIN
    // 거부: COMPANY_MANAGER, HUB_DELIVERY_MANAGER, COM_DELIVERY_MANAGER
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("상품 삭제 권한")
    class DeleteProductAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            willDoNothing().given(productService).deleteProduct(any(), any());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "MASTER_ADMIN"))
                .andDo(print())
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("HUB_ADMIN - 허용")
        void hubAdmin_allowed() throws Exception {
            willDoNothing().given(productService).deleteProduct(any(), any());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN"))
                .andDo(print())
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 거부 (403)")
        void companyManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("product-delete-forbidden",
                    responseFields(
                        fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (FORBIDDEN)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                    )
                ));
        }

        @Test
        @DisplayName("HUB_DELIVERY_MANAGER - 거부 (403)")
        void hubDeliveryManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_DELIVERY_MANAGER"))
                .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("COM_DELIVERY_MANAGER - 거부 (403)")
        void comDeliveryManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/products/{productId}", PRODUCT_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COM_DELIVERY_MANAGER"))
                .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/products/{productId}", PRODUCT_ID))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }
}
