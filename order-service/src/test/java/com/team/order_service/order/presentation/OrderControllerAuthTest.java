package com.team.order_service.order.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.common.exception.GlobalExceptionHandler;
import com.team.order_service.global.config.AuditConfig;
import com.team.order_service.global.config.SecurityConfig;
import com.team.order_service.order.application.OrderService;
import com.team.order_service.order.application.dto.OrderItemResult;
import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.domain.OrderStatus;
import com.team.order_service.order.presentation.dto.OrderCreateRequest;
import com.team.order_service.order.presentation.dto.OrderItemRequest;
import com.team.order_service.order.presentation.dto.OrderUpdateRequest;
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
import java.util.List;
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

@WebMvcTest(OrderController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, AuditConfig.class, GlobalExceptionHandler.class})
class OrderControllerAuthTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    OrderService orderService;

    private MockMvc mockMvc;

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID SUPPLIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RECEIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID DELIVERY_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

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

    private OrderResult orderResult() {
        return new OrderResult(
            ORDER_ID, USER_ID, SUPPLIER_ID, RECEIVER_ID, DELIVERY_ID,
            LocalDateTime.of(2026, 12, 31, 18, 0),
            "문 앞에 놓아주세요",
            new BigDecimal("15000.00"),
            OrderStatus.CREATED,
            null, null,
            List.of(new OrderItemResult(ITEM_ID, PRODUCT_ID, "타이레놀 500mg", new BigDecimal("5000.00"), 3)),
            LocalDateTime.of(2026, 4, 6, 0, 0), USER_ID,
            null, null
        );
    }

    private String createRequestBody() throws Exception {
        return objectMapper.writeValueAsString(new OrderCreateRequest(
            SUPPLIER_ID, RECEIVER_ID,
            LocalDateTime.of(2026, 12, 31, 18, 0),
            null,
            List.of(new OrderItemRequest(PRODUCT_ID, 3))
        ));
    }

    private String updateRequestBody() throws Exception {
        return objectMapper.writeValueAsString(
            new OrderUpdateRequest(null, "요청 사항 수정")
        );
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 주문 생성 권한 테스트
    // 허용: MASTER_ADMIN, HUB_ADMIN, HUB_DELIVERY_MANAGER, COMPANY_MANAGER
    // 거부: COM_DELIVERY_MANAGER
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("주문 생성 권한")
    class CreateOrderAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            given(orderService.createOrder(any())).willReturn(orderResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/orders")
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
            given(orderService.createOrder(any())).willReturn(orderResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/orders")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("HUB_DELIVERY_MANAGER - 허용")
        void hubDeliveryManager_allowed() throws Exception {
            given(orderService.createOrder(any())).willReturn(orderResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/orders")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 허용")
        void companyManager_allowed() throws Exception {
            given(orderService.createOrder(any())).willReturn(orderResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/orders")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("COM_DELIVERY_MANAGER - 거부 (403)")
        void comDeliveryManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/orders")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COM_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("order-create-forbidden",
                    responseFields(
                        fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (FORBIDDEN)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                    )
                ));
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody()))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andDo(document("order-create-unauthorized",
                    responseFields(
                        fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (UNAUTHORIZED)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                    )
                ));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 주문 수정 권한 테스트
    // 허용: MASTER_ADMIN, HUB_ADMIN
    // 거부: HUB_DELIVERY_MANAGER, COMPANY_MANAGER, COM_DELIVERY_MANAGER
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("주문 수정 권한")
    class UpdateOrderAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            given(orderService.updateOrder(any())).willReturn(orderResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}", ORDER_ID)
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
            given(orderService.updateOrder(any())).willReturn(orderResult());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 거부 (403)")
        void companyManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("order-update-forbidden",
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
                    .patch("/api/v1/orders/{orderId}", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_DELIVERY_MANAGER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}", ORDER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 주문 삭제 권한 테스트
    // 허용: MASTER_ADMIN, HUB_ADMIN
    // 거부: HUB_DELIVERY_MANAGER, COMPANY_MANAGER, COM_DELIVERY_MANAGER
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("주문 삭제 권한")
    class DeleteOrderAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            willDoNothing().given(orderService).deleteOrder(any(), any());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/orders/{orderId}", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "MASTER_ADMIN"))
                .andDo(print())
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("HUB_ADMIN - 허용")
        void hubAdmin_allowed() throws Exception {
            willDoNothing().given(orderService).deleteOrder(any(), any());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/orders/{orderId}", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN"))
                .andDo(print())
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 거부 (403)")
        void companyManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/orders/{orderId}", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("order-delete-forbidden",
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
                    .delete("/api/v1/orders/{orderId}", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_DELIVERY_MANAGER"))
                .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .delete("/api/v1/orders/{orderId}", ORDER_ID))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 주문 취소 권한 테스트
    // SecurityConfig에서 PATCH /api/v1/orders/** → MASTER_ADMIN, HUB_ADMIN
    // ══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("주문 취소 권한")
    class CancelOrderAuth {

        @Test
        @DisplayName("MASTER_ADMIN - 허용")
        void masterAdmin_allowed() throws Exception {
            willDoNothing().given(orderService).cancelOrder(any(), any());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "MASTER_ADMIN"))
                .andDo(print())
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("HUB_ADMIN - 허용")
        void hubAdmin_allowed() throws Exception {
            willDoNothing().given(orderService).cancelOrder(any(), any());

            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "HUB_ADMIN"))
                .andDo(print())
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("COMPANY_MANAGER - 거부 (403)")
        void companyManager_forbidden() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", "COMPANY_MANAGER"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andDo(document("order-cancel-forbidden",
                    responseFields(
                        fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (FORBIDDEN)"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                    )
                ));
        }

        @Test
        @DisplayName("헤더 없음 - 인증 실패 (401)")
        void noHeader_unauthorized() throws Exception {
            mockMvc.perform(RestDocumentationRequestBuilders
                    .patch("/api/v1/orders/{orderId}/cancel", ORDER_ID))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }
}
