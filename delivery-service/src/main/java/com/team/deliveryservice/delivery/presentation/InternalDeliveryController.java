package com.team.deliveryservice.delivery.presentation;

import com.team.deliveryservice.delivery.application.dto.request.ChangeDeliveryStatusRequest;
import com.team.deliveryservice.delivery.application.dto.request.CreateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.response.AiDeliveryResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.delivery.application.service.DeliveryService;
import com.team.deliveryservice.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/deliveries")
public class InternalDeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryResponse>> create(
        @Valid @RequestBody CreateDeliveryRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(deliveryService.createDelivery(request)));
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> get(
        @PathVariable UUID deliveryId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.getDelivery(deliveryId)
        ));
    }

    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<ApiResponse<DeliveryResponse>> changeStatus(
        @PathVariable UUID deliveryId,
        @Valid @RequestBody ChangeDeliveryStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.changeDeliveryStatus(deliveryId, request)
        ));
    }

    @PatchMapping("/{deliveryId}/cancel")
    public ResponseEntity<ApiResponse<DeliveryResponse>> cancel(
        @PathVariable UUID deliveryId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.cancelDelivery(deliveryId)
        ));
    }

    @DeleteMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable UUID deliveryId
    ) {
        deliveryService.deleteDelivery(deliveryId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/{deliveryId}/ai")
    public ResponseEntity<ApiResponse<AiDeliveryResponse>> getAiInfo(
        @PathVariable UUID deliveryId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.getAiDeliveryInfo(deliveryId)
        ));
    }
}
