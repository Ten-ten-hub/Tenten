package com.team.order_service.order.presentation;

import com.team.order_service.order.application.OrderService;
import com.team.order_service.order.presentation.dto.OrderStatusUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/orders")
public class OrderInternalController {

    private final OrderService orderService;

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
        @PathVariable UUID orderId,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        orderService.updateOrderStatus(orderId, request.orderStatus());
        return ResponseEntity.noContent().build();
    }
}
