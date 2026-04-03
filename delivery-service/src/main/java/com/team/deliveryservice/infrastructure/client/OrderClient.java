package com.team.deliveryservice.infrastructure.client;

import com.team.deliveryservice.global.config.FeignRetryConfig;
import com.team.deliveryservice.infrastructure.client.dto.OrderInternalResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "order-service",
    configuration = FeignRetryConfig.class
)
public interface OrderClient {

    @GetMapping("/internal/v1/orders/{orderId}")
    OrderInternalResponse getOrder(@PathVariable UUID orderId);
}
