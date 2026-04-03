package com.team.order_service.order.presentation;

import com.team.order_service.order.application.OrderService;
import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.presentation.dto.OrderStatusUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/orders")
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResult> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
        @PathVariable UUID orderId,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        orderService.updateOrderStatus(orderId, request.orderStatus());
        return ResponseEntity.noContent().build();
    }
}
