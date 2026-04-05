package com.team.deliveryservice.delivery.presentation;

import com.team.deliveryservice.delivery.application.dto.request.AssignCompanyDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.AssignHubDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.UpdateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryPageResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.delivery.application.search.DeliverySearchCondition;
import com.team.deliveryservice.delivery.application.service.DeliveryService;
import com.team.deliveryservice.global.auth.RequireRole;
import com.team.deliveryservice.global.common.ApiResponse;
import com.team.deliveryservice.global.common.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class ExternalDeliveryController {

    private final DeliveryService deliveryService;

    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN", "COMPANY_MANAGER", "HUB_DELIVERY_MANAGER", "COM_DELIVERY_MANAGER"})
    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> get(
        @PathVariable UUID deliveryId,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.getDelivery(deliveryId, currentUser)
        ));
    }

    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN", "COMPANY_MANAGER", "HUB_DELIVERY_MANAGER", "COM_DELIVERY_MANAGER"})
    @GetMapping
    public ResponseEntity<ApiResponse<DeliveryPageResponse>> search(
        @ModelAttribute DeliverySearchCondition condition,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.searchDeliveries(condition, currentUser)
        ));
    }

    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN", "HUB_DELIVERY_MANAGER", "COM_DELIVERY_MANAGER"})
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

    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN"})
    @PatchMapping("/{deliveryId}/assign-company-manager")
    public ResponseEntity<ApiResponse<DeliveryResponse>> assignCompanyManager(
        @PathVariable UUID deliveryId,
        @Valid @RequestBody AssignCompanyDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.assignCompanyDeliveryManager(deliveryId, request, currentUser)
        ));
    }

    @RequireRole({"MASTER_ADMIN", "HUB_ADMIN"})
    @PatchMapping("/{deliveryId}/assign-delivery-manager")
    public ResponseEntity<ApiResponse<DeliveryResponse>> assignDeliveryManager(
        @PathVariable UUID deliveryId,
        @Valid @RequestBody AssignHubDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryService.assignHubDeliveryManager(deliveryId, request, currentUser)
        ));
    }
}
