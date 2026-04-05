package com.team.order_service.order.application;

import com.team.order_service.order.application.dto.OrderCreateCommand;
import com.team.order_service.order.application.dto.OrderItemCommand;
import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.domain.Order;
import com.team.order_service.order.domain.OrderRepository;
import com.team.order_service.order.infrastructure.client.DeliveryClient;
import com.team.order_service.order.infrastructure.client.ProductClient;
import com.team.order_service.order.infrastructure.client.dto.DeliveryApiResponse;
import com.team.order_service.order.infrastructure.client.dto.DeliveryCreateRequest;
import com.team.order_service.order.infrastructure.client.dto.DeliveryResponse;
import com.team.order_service.order.infrastructure.client.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private DeliveryClient deliveryClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("주문 생성 성공 - 주문 생성 후 배송도 함께 생성되고 deliveryId 가 반영된다")
    void create_order_success_with_delivery_creation() {
        // given
        UUID orderedBy = UUID.randomUUID();
        UUID supplierCompanyId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        LocalDateTime deadlineAt = LocalDateTime.of(2026, 4, 10, 18, 0);

        OrderCreateCommand command = new OrderCreateCommand(
            orderedBy,
            supplierCompanyId,
            receiverCompanyId,
            deadlineAt,
            "문 앞에 놓아주세요",
            List.of(new OrderItemCommand(productId, 3))
        );

        given(orderRepository.save(any(Order.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        given(productClient.getProduct(eq(orderedBy), eq(productId)))
            .willReturn(new ProductResponse(
                productId,
                "타이레놀",
                BigDecimal.valueOf(5000)
            ));

        given(deliveryClient.createDelivery(any(DeliveryCreateRequest.class)))
            .willReturn(new DeliveryApiResponse(
                true,
                new DeliveryResponse(deliveryId),
                "SUCCESS",
                "요청이 성공했습니다."
            ));

        // when
        OrderResult result = orderService.createOrder(command);

        // then
        assertThat(result.deliveryId()).isEqualTo(deliveryId);
        assertThat(result.orderedBy()).isEqualTo(orderedBy);
        assertThat(result.supplierCompanyId()).isEqualTo(supplierCompanyId);
        assertThat(result.receiverCompanyId()).isEqualTo(receiverCompanyId);
        assertThat(result.orderItems()).hasSize(1);

        ArgumentCaptor<DeliveryCreateRequest> requestCaptor =
            ArgumentCaptor.forClass(DeliveryCreateRequest.class);

        verify(deliveryClient).createDelivery(requestCaptor.capture());

        DeliveryCreateRequest captured = requestCaptor.getValue();
        assertThat(captured.orderId()).isEqualTo(result.id());
        assertThat(captured.orderedBy()).isEqualTo(orderedBy);
        assertThat(captured.supplierCompanyId()).isEqualTo(supplierCompanyId);
        assertThat(captured.receiverCompanyId()).isEqualTo(receiverCompanyId);
        assertThat(captured.deadlineAt()).isEqualTo(deadlineAt);
        assertThat(captured.requestNote()).isEqualTo("문 앞에 놓아주세요");
    }
}
