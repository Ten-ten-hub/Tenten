package com.team.order_service.order.infrastructure.client;

import com.team.order_service.order.infrastructure.client.dto.DeliveryCreateRequest;
import com.team.order_service.order.infrastructure.client.dto.DeliveryResponse;
import com.team.order_service.order.infrastructure.client.dto.DeliveryStatusRequest;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "delivery-service")
public interface DeliveryClient {

    @PostMapping("/internal/v1/deliveries")
    DeliveryResponse createDelivery(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @RequestBody DeliveryCreateRequest request
    );

    @PatchMapping("/internal/v1/deliveries/{deliveryId}/status")
    void updateDeliveryStatus(
        @PathVariable UUID deliveryId,
        @RequestBody DeliveryStatusRequest request
    );

    @PatchMapping("/internal/v1/deliveries/{deliveryId}/cancel")
    void cancelDelivery(
        @RequestHeader("X-User-Id") UUID requestUserId,
        @PathVariable UUID deliveryId
    );

    @DeleteMapping("/internal/v1/deliveries/{deliveryId}")
    void deleteDelivery(
        @PathVariable UUID deliveryId
    );
}
