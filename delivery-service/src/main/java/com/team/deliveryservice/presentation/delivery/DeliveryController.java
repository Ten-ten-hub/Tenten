package com.team.deliveryservice.presentation.delivery;

import com.team.deliveryservice.application.delivery.CreateDeliveryRequest;
import com.team.deliveryservice.application.delivery.DeliveryPageResponse;
import com.team.deliveryservice.application.delivery.DeliveryResponse;
import com.team.deliveryservice.application.delivery.DeliverySearchCondition;
import com.team.deliveryservice.application.delivery.DeliveryService;
import com.team.deliveryservice.application.delivery.UpdateDeliveryRequest;
import com.team.deliveryservice.presentation.common.ApiResponse;
import com.team.deliveryservice.presentation.common.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryResponse>> create(
        @Valid @RequestBody CreateDeliveryRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(deliveryService.createDelivery(request, currentUser)));
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> get(
        @PathVariable UUID deliveryId,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.getDelivery(deliveryId, currentUser)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DeliveryPageResponse>> search(
        @ModelAttribute DeliverySearchCondition condition,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.searchDeliveries(condition, currentUser)
        ));
    }

    @PutMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> update(
        @PathVariable UUID deliveryId,
        @Valid @RequestBody UpdateDeliveryRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.updateDelivery(deliveryId, request, currentUser)
        ));
    }

    @DeleteMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable UUID deliveryId,
        CurrentUser currentUser
    ) {
        deliveryService.deleteDelivery(deliveryId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
