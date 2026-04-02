package com.team.order_service.order.infrastructure.client;

import com.team.order_service.order.infrastructure.client.dto.DeliveryCreateRequest;
import com.team.order_service.order.infrastructure.client.dto.DeliveryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "delivery-service")
public interface DeliveryClient {

    @PostMapping("/internal/v1/deliveries")
    DeliveryResponse createDelivery(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @RequestBody DeliveryCreateRequest request
    );

    @DeleteMapping("/internal/v1/deliveries/{deliveryId}")
    void cancelDelivery(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @PathVariable UUID deliveryId
    );
}
