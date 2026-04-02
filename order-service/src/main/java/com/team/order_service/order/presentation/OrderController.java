package com.team.order_service.order.presentation;

import com.team.order_service.global.dto.PageResponse;
import com.team.order_service.order.application.OrderService;
import com.team.order_service.order.application.dto.OrderResult;
import com.team.order_service.order.presentation.dto.OrderCreateRequest;
import com.team.order_service.order.presentation.dto.OrderGetRequest;
import com.team.order_service.order.presentation.dto.OrderResponse;
import com.team.order_service.order.presentation.dto.OrderUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @RequestHeader("X-User-Role") String requestUserRole,
        @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderResult result = orderService.createOrder(request.toCommand(requestUserId));
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(result));
    }

    @GetMapping("/{orderId}")
    ResponseEntity<OrderResponse> getOrder(
        @PathVariable UUID orderId
    ) {
        OrderResult result = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(result));
    }

    @GetMapping
    ResponseEntity<PageResponse<OrderResponse>> getOrders(
        @ModelAttribute OrderGetRequest request,
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        Pageable validatedPageable = validatePageSize(pageable);
        Page<OrderResult> results = orderService.getOrders(request.toQuery(), validatedPageable);
        return ResponseEntity.ok(PageResponse.from(results.map(OrderResponse::from)));
    }

    @PatchMapping("/{orderId}")
    ResponseEntity<OrderResponse> updateOrder(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @RequestHeader("X-User-Role") String requestUserRole,
        @PathVariable UUID orderId,
        @Valid @RequestBody OrderUpdateRequest request
    ) {
        OrderResult result = orderService.updateOrder(request.toCommand(orderId));
        return ResponseEntity.ok(OrderResponse.from(result));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @RequestHeader("X-User-Role") String requestUserRole,
        @PathVariable UUID orderId
    ) {
        orderService.cancelOrder(orderId, requestUserId);
        return ResponseEntity.noContent().build();
    }

    private Pageable validatePageSize(Pageable pageable) {
        int size = pageable.getPageSize();
        if (size != 10 && size != 30 && size != 50) {
            return PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());
        }
        return pageable;
    }
}
