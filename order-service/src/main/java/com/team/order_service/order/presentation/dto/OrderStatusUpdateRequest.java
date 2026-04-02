package com.team.order_service.order.presentation.dto;

import com.team.order_service.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
    @NotNull(message = "주문 상태는 필수입니다.")
    OrderStatus orderStatus
) {
}
