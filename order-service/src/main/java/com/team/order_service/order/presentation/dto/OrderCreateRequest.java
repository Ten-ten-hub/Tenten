package com.team.order_service.order.presentation.dto;

import com.team.order_service.order.application.dto.OrderCreateCommand;
import com.team.order_service.order.application.dto.OrderItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(

    @NotNull(message = "공급 업체 ID는 필수입니다.")
    UUID supplierCompanyId,

    @NotNull(message = "수령 업체 ID는 필수입니다.")
    UUID receiverCompanyId,

    @NotNull(message = "납기 일시는 필수입니다.")
    LocalDateTime deadlineAt,

    String requestNote,

    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    List<OrderItemRequest> orderItems
) {
    public OrderCreateCommand toCommand(UUID orderedBy) {
        return new OrderCreateCommand(
            orderedBy,
            supplierCompanyId,
            receiverCompanyId,
            deadlineAt,
            requestNote,
            orderItems.stream()
                .map(item -> new OrderItemCommand(item.productId(), item.quantity())).toList()
        );
    }

}
