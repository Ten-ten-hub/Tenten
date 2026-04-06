package com.team.order_service.order.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.common.exception.BusinessException;
import com.team.common.exception.GlobalExceptionHandler;
import com.team.order_service.global.config.AuditConfig;
import com.team.order_service.global.config.SecurityConfig;
import com.team.order_service.global.exception.OrderErrorCode;
import com.team.order_service.order.application.OrderService;
import com.team.order_service.order.application.dto.OrderItemResult;
import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.domain.OrderStatus;
import com.team.order_service.order.presentation.dto.OrderCreateRequest;
import com.team.order_service.order.presentation.dto.OrderItemRequest;
import com.team.order_service.order.presentation.dto.OrderUpdateRequest;
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

@WebMvcTest(OrderController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, AuditConfig.class, GlobalExceptionHandler.class})
class OrderControllerDocsTest {

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

    // ══════════════════════════════════════════════════════════════════════════════
    // POST /api/v1/orders  —  주문 생성
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("주문 생성 - 성공")
    @WithMockUser(roles = "COMPANY_MANAGER")
    void createOrder_success() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
            SUPPLIER_ID, RECEIVER_ID,
            LocalDateTime.of(2026, 12, 31, 18, 0),
            "문 앞에 놓아주세요",
            List.of(new OrderItemRequest(PRODUCT_ID, 3))
        );
        given(orderService.createOrder(any())).willReturn(orderResult());

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/orders")
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "COMPANY_MANAGER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
            .andDo(document("order-create",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN, HUB_DELIVERY_MANAGER, COMPANY_MANAGER)")
                ),
                requestFields(
                    fieldWithPath("supplierCompanyId").type(JsonFieldType.STRING).description("공급 업체 UUID (필수)"),
                    fieldWithPath("receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 UUID (필수)"),
                    fieldWithPath("deadlineAt").type(JsonFieldType.STRING).description("납기 일시 (필수)"),
                    fieldWithPath("requestNote").type(JsonFieldType.STRING).optional().description("요청 사항 (선택)"),
                    fieldWithPath("orderItems").type(JsonFieldType.ARRAY).description("주문 상품 목록 (최소 1개)"),
                    fieldWithPath("orderItems[].productId").type(JsonFieldType.STRING).description("상품 UUID (필수)"),
                    fieldWithPath("orderItems[].quantity").type(JsonFieldType.NUMBER).description("수량 (필수, 1 이상)")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("주문 UUID"),
                    fieldWithPath("orderedBy").type(JsonFieldType.STRING).description("주문자 UUID"),
                    fieldWithPath("supplierCompanyId").type(JsonFieldType.STRING).description("공급 업체 UUID"),
                    fieldWithPath("receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 UUID"),
                    fieldWithPath("deliveryId").type(JsonFieldType.STRING).optional().description("배송 UUID"),
                    fieldWithPath("deadlineAt").type(JsonFieldType.STRING).description("납기 일시"),
                    fieldWithPath("requestNote").type(JsonFieldType.STRING).optional().description("요청 사항"),
                    fieldWithPath("totalPrice").type(JsonFieldType.NUMBER).description("총 주문 금액"),
                    fieldWithPath("orderStatus").type(JsonFieldType.STRING).description("주문 상태 (CREATED, CONFIRMED, READY_FOR_DELIVERY, IN_DELIVERY, CANCELLED, COMPLETED)"),
                    fieldWithPath("cancelledAt").type(JsonFieldType.STRING).optional().description("취소 일시"),
                    fieldWithPath("cancelledBy").type(JsonFieldType.STRING).optional().description("취소자 UUID"),
                    fieldWithPath("orderItems").type(JsonFieldType.ARRAY).description("주문 상품 목록"),
                    fieldWithPath("orderItems[].id").type(JsonFieldType.STRING).description("주문 아이템 UUID"),
                    fieldWithPath("orderItems[].productId").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("orderItems[].productNameSnapshot").type(JsonFieldType.STRING).description("주문 시점 상품명"),
                    fieldWithPath("orderItems[].unitPriceSnapshot").type(JsonFieldType.NUMBER).description("주문 시점 단가"),
                    fieldWithPath("orderItems[].quantity").type(JsonFieldType.NUMBER).description("수량"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).optional().description("수정 일시"),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).optional().description("수정자 UUID")
                )
            ));
    }

    @Test
    @DisplayName("주문 생성 - 실패 (유효성 검증 - 주문 상품 없음)")
    @WithMockUser(roles = "COMPANY_MANAGER")
    void createOrder_validationFail() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
            SUPPLIER_ID, RECEIVER_ID,
            LocalDateTime.of(2026, 12, 31, 18, 0),
            null,
            List.of()
        );

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/orders")
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "COMPANY_MANAGER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andDo(document("order-create-validation-fail",
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
    @DisplayName("주문 생성 - 실패 (상품 없음 404)")
    @WithMockUser(roles = "COMPANY_MANAGER")
    void createOrder_productNotFound() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
            SUPPLIER_ID, RECEIVER_ID,
            LocalDateTime.of(2026, 12, 31, 18, 0),
            null,
            List.of(new OrderItemRequest(PRODUCT_ID, 3))
        );
        given(orderService.createOrder(any()))
            .willThrow(new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/orders")
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "COMPANY_MANAGER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
            .andDo(document("order-create-product-not-found",
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (PRODUCT_NOT_FOUND)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    @Test
    @DisplayName("주문 생성 - 실패 (재고 차감 실패 500)")
    @WithMockUser(roles = "COMPANY_MANAGER")
    void createOrder_stockDeductFailed() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
            SUPPLIER_ID, RECEIVER_ID,
            LocalDateTime.of(2026, 12, 31, 18, 0),
            null,
            List.of(new OrderItemRequest(PRODUCT_ID, 3))
        );
        given(orderService.createOrder(any()))
            .willThrow(new BusinessException(OrderErrorCode.STOCK_DEDUCT_FAILED));

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/orders")
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "COMPANY_MANAGER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("STOCK_DEDUCT_FAILED"))
            .andDo(document("order-create-stock-deduct-failed",
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (STOCK_DEDUCT_FAILED)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GET /api/v1/orders/{orderId}  —  주문 단건 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("주문 단건 조회 - 성공")
    @WithMockUser(roles = "COMPANY_MANAGER")
    void getOrder_success() throws Exception {
        given(orderService.getOrder(ORDER_ID)).willReturn(orderResult());

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/orders/{orderId}", ORDER_ID))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
            .andDo(document("order-get",
                pathParameters(
                    parameterWithName("orderId").description("조회할 주문 UUID")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("주문 UUID"),
                    fieldWithPath("orderedBy").type(JsonFieldType.STRING).description("주문자 UUID"),
                    fieldWithPath("supplierCompanyId").type(JsonFieldType.STRING).description("공급 업체 UUID"),
                    fieldWithPath("receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 UUID"),
                    fieldWithPath("deliveryId").type(JsonFieldType.STRING).optional().description("배송 UUID"),
                    fieldWithPath("deadlineAt").type(JsonFieldType.STRING).description("납기 일시"),
                    fieldWithPath("requestNote").type(JsonFieldType.STRING).optional().description("요청 사항"),
                    fieldWithPath("totalPrice").type(JsonFieldType.NUMBER).description("총 주문 금액"),
                    fieldWithPath("orderStatus").type(JsonFieldType.STRING).description("주문 상태"),
                    fieldWithPath("cancelledAt").type(JsonFieldType.STRING).optional().description("취소 일시"),
                    fieldWithPath("cancelledBy").type(JsonFieldType.STRING).optional().description("취소자 UUID"),
                    fieldWithPath("orderItems").type(JsonFieldType.ARRAY).description("주문 상품 목록"),
                    fieldWithPath("orderItems[].id").type(JsonFieldType.STRING).description("주문 아이템 UUID"),
                    fieldWithPath("orderItems[].productId").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("orderItems[].productNameSnapshot").type(JsonFieldType.STRING).description("주문 시점 상품명"),
                    fieldWithPath("orderItems[].unitPriceSnapshot").type(JsonFieldType.NUMBER).description("주문 시점 단가"),
                    fieldWithPath("orderItems[].quantity").type(JsonFieldType.NUMBER).description("수량"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).optional().description("수정 일시"),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).optional().description("수정자 UUID")
                )
            ));
    }

    @Test
    @DisplayName("주문 단건 조회 - 실패 (존재하지 않는 주문 404)")
    @WithMockUser(roles = "COMPANY_MANAGER")
    void getOrder_notFound() throws Exception {
        given(orderService.getOrder(ORDER_ID))
            .willThrow(new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/orders/{orderId}", ORDER_ID))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
            .andDo(document("order-get-not-found",
                pathParameters(
                    parameterWithName("orderId").description("조회할 주문 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (ORDER_NOT_FOUND)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GET /api/v1/orders  —  주문 목록 조회
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("주문 목록 조회 - 성공")
    @WithMockUser(roles = "MASTER_ADMIN")
    void getOrders_success() throws Exception {
        PageImpl<OrderResult> page = new PageImpl<>(
            List.of(orderResult()), PageRequest.of(0, 10), 1
        );
        given(orderService.getOrders(any(), any())).willReturn(page);

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/orders")
                .param("orderStatus", "CREATED")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andDo(document("order-list",
                queryParameters(
                    parameterWithName("orderedBy").optional().description("주문자 UUID 필터"),
                    parameterWithName("supplierCompanyId").optional().description("공급 업체 UUID 필터"),
                    parameterWithName("receiverCompanyId").optional().description("수령 업체 UUID 필터"),
                    parameterWithName("orderStatus").optional().description("주문 상태 필터 (CREATED, CONFIRMED, READY_FOR_DELIVERY, IN_DELIVERY, CANCELLED, COMPLETED)"),
                    parameterWithName("page").optional().description("페이지 번호 (0부터 시작, 기본값 0)"),
                    parameterWithName("size").optional().description("페이지 크기 (10, 30, 50, 기본값 10)")
                ),
                responseFields(
                    fieldWithPath("content").type(JsonFieldType.ARRAY).description("주문 목록"),
                    fieldWithPath("content[].id").type(JsonFieldType.STRING).description("주문 UUID"),
                    fieldWithPath("content[].orderedBy").type(JsonFieldType.STRING).description("주문자 UUID"),
                    fieldWithPath("content[].supplierCompanyId").type(JsonFieldType.STRING).description("공급 업체 UUID"),
                    fieldWithPath("content[].receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 UUID"),
                    fieldWithPath("content[].deliveryId").type(JsonFieldType.STRING).optional().description("배송 UUID"),
                    fieldWithPath("content[].deadlineAt").type(JsonFieldType.STRING).description("납기 일시"),
                    fieldWithPath("content[].requestNote").type(JsonFieldType.STRING).optional().description("요청 사항"),
                    fieldWithPath("content[].totalPrice").type(JsonFieldType.NUMBER).description("총 주문 금액"),
                    fieldWithPath("content[].orderStatus").type(JsonFieldType.STRING).description("주문 상태"),
                    fieldWithPath("content[].cancelledAt").type(JsonFieldType.STRING).optional().description("취소 일시"),
                    fieldWithPath("content[].cancelledBy").type(JsonFieldType.STRING).optional().description("취소자 UUID"),
                    fieldWithPath("content[].orderItems").type(JsonFieldType.ARRAY).description("주문 상품 목록"),
                    fieldWithPath("content[].orderItems[].id").type(JsonFieldType.STRING).description("주문 아이템 UUID"),
                    fieldWithPath("content[].orderItems[].productId").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("content[].orderItems[].productNameSnapshot").type(JsonFieldType.STRING).description("주문 시점 상품명"),
                    fieldWithPath("content[].orderItems[].unitPriceSnapshot").type(JsonFieldType.NUMBER).description("주문 시점 단가"),
                    fieldWithPath("content[].orderItems[].quantity").type(JsonFieldType.NUMBER).description("수량"),
                    fieldWithPath("content[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("content[].createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("content[].updatedAt").type(JsonFieldType.STRING).optional().description("수정 일시"),
                    fieldWithPath("content[].updatedBy").type(JsonFieldType.STRING).optional().description("수정자 UUID"),
                    fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 주문 수"),
                    fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                    fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                    fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/orders/{orderId}  —  주문 수정
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("주문 수정 - 성공")
    @WithMockUser(roles = "MASTER_ADMIN")
    void updateOrder_success() throws Exception {
        OrderUpdateRequest request = new OrderUpdateRequest(
            LocalDateTime.of(2027, 1, 31, 18, 0),
            "경비실에 맡겨주세요"
        );
        given(orderService.updateOrder(any())).willReturn(orderResult());

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/orders/{orderId}", ORDER_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("order-update",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN)")
                ),
                pathParameters(
                    parameterWithName("orderId").description("수정할 주문 UUID")
                ),
                requestFields(
                    fieldWithPath("deadlineAt").type(JsonFieldType.STRING).optional().description("납기 일시 (변경 시에만 입력)"),
                    fieldWithPath("requestNote").type(JsonFieldType.STRING).optional().description("요청 사항 (변경 시에만 입력)")
                ),
                responseFields(
                    fieldWithPath("id").type(JsonFieldType.STRING).description("주문 UUID"),
                    fieldWithPath("orderedBy").type(JsonFieldType.STRING).description("주문자 UUID"),
                    fieldWithPath("supplierCompanyId").type(JsonFieldType.STRING).description("공급 업체 UUID"),
                    fieldWithPath("receiverCompanyId").type(JsonFieldType.STRING).description("수령 업체 UUID"),
                    fieldWithPath("deliveryId").type(JsonFieldType.STRING).optional().description("배송 UUID"),
                    fieldWithPath("deadlineAt").type(JsonFieldType.STRING).description("납기 일시"),
                    fieldWithPath("requestNote").type(JsonFieldType.STRING).optional().description("요청 사항"),
                    fieldWithPath("totalPrice").type(JsonFieldType.NUMBER).description("총 주문 금액"),
                    fieldWithPath("orderStatus").type(JsonFieldType.STRING).description("주문 상태"),
                    fieldWithPath("cancelledAt").type(JsonFieldType.STRING).optional().description("취소 일시"),
                    fieldWithPath("cancelledBy").type(JsonFieldType.STRING).optional().description("취소자 UUID"),
                    fieldWithPath("orderItems").type(JsonFieldType.ARRAY).description("주문 상품 목록"),
                    fieldWithPath("orderItems[].id").type(JsonFieldType.STRING).description("주문 아이템 UUID"),
                    fieldWithPath("orderItems[].productId").type(JsonFieldType.STRING).description("상품 UUID"),
                    fieldWithPath("orderItems[].productNameSnapshot").type(JsonFieldType.STRING).description("주문 시점 상품명"),
                    fieldWithPath("orderItems[].unitPriceSnapshot").type(JsonFieldType.NUMBER).description("주문 시점 단가"),
                    fieldWithPath("orderItems[].quantity").type(JsonFieldType.NUMBER).description("수량"),
                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                    fieldWithPath("createdBy").type(JsonFieldType.STRING).description("생성자 UUID"),
                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).optional().description("수정 일시"),
                    fieldWithPath("updatedBy").type(JsonFieldType.STRING).optional().description("수정자 UUID")
                )
            ));
    }

    @Test
    @DisplayName("주문 수정 - 실패 (존재하지 않는 주문 404)")
    @WithMockUser(roles = "MASTER_ADMIN")
    void updateOrder_notFound() throws Exception {
        OrderUpdateRequest request = new OrderUpdateRequest(null, "요청 사항 수정");
        given(orderService.updateOrder(any()))
            .willThrow(new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/orders/{orderId}", ORDER_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
            .andDo(document("order-update-not-found",
                pathParameters(
                    parameterWithName("orderId").description("수정할 주문 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (ORDER_NOT_FOUND)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/orders/{orderId}  —  주문 삭제
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("주문 삭제 - 성공")
    @WithMockUser(roles = "MASTER_ADMIN")
    void deleteOrder_success() throws Exception {
        willDoNothing().given(orderService).deleteOrder(any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders
                .delete("/api/v1/orders/{orderId}", ORDER_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("order-delete",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN)")
                ),
                pathParameters(
                    parameterWithName("orderId").description("삭제할 주문 UUID")
                )
            ));
    }

    @Test
    @DisplayName("주문 삭제 - 실패 (취소되지 않은 주문 400)")
    @WithMockUser(roles = "MASTER_ADMIN")
    void deleteOrder_notDeletable() throws Exception {
        willThrow(new BusinessException(OrderErrorCode.ORDER_NOT_DELETABLE))
            .given(orderService).deleteOrder(any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders
                .delete("/api/v1/orders/{orderId}", ORDER_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_DELETABLE"))
            .andDo(document("order-delete-not-deletable",
                pathParameters(
                    parameterWithName("orderId").description("삭제할 주문 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (ORDER_NOT_DELETABLE)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/orders/{orderId}/cancel  —  주문 취소
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("주문 취소 - 성공")
    @WithMockUser(roles = "MASTER_ADMIN")
    void cancelOrder_success() throws Exception {
        willDoNothing().given(orderService).cancelOrder(any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("order-cancel",
                requestHeaders(
                    headerWithName("X-User-Id").description("요청자 UUID"),
                    headerWithName("X-User-Role").description("요청자 권한 (MASTER_ADMIN, HUB_ADMIN)")
                ),
                pathParameters(
                    parameterWithName("orderId").description("취소할 주문 UUID")
                )
            ));
    }

    @Test
    @DisplayName("주문 취소 - 실패 (취소 불가 상태 400)")
    @WithMockUser(roles = "MASTER_ADMIN")
    void cancelOrder_notCancellable() throws Exception {
        willThrow(new BusinessException(OrderErrorCode.ORDER_NOT_CANCELLABLE))
            .given(orderService).cancelOrder(any(), any());

        mockMvc.perform(RestDocumentationRequestBuilders
                .patch("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                .header("X-User-Id", USER_ID.toString())
                .header("X-User-Role", "MASTER_ADMIN"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"))
            .andDo(document("order-cancel-not-cancellable",
                pathParameters(
                    parameterWithName("orderId").description("취소할 주문 UUID")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드 (ORDER_NOT_CANCELLABLE)"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("details").type(JsonFieldType.NULL).optional().description("상세 정보")
                )
            ));
    }
}
