package com.team.deliveryservice.deliverymanager.presentation;

import com.team.deliveryservice.deliverymanager.application.dto.request.CreateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerPageResponse;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerResponse;
import com.team.deliveryservice.deliverymanager.application.search.DeliveryManagerSearchCondition;
import com.team.deliveryservice.deliverymanager.application.service.DeliveryManagerService;
import com.team.deliveryservice.deliverymanager.application.dto.request.UpdateDeliveryManagerRequest;
import com.team.deliveryservice.global.common.ApiResponse;
import com.team.deliveryservice.global.common.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery-managers")
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryManagerResponse>> create(
        @Valid @RequestBody CreateDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(deliveryManagerService.createDeliveryManager(request, currentUser)));
    }

    @GetMapping("/{deliveryManagerId}")
    public ResponseEntity<ApiResponse<DeliveryManagerResponse>> get(
        @PathVariable UUID deliveryManagerId,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryManagerService.getDeliveryManager(deliveryManagerId, currentUser)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DeliveryManagerPageResponse>> search(
        @ModelAttribute DeliveryManagerSearchCondition condition,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryManagerService.searchDeliveryManagers(condition, currentUser)
        ));
    }

    @PutMapping("/{deliveryManagerId}")
    public ResponseEntity<ApiResponse<DeliveryManagerResponse>> update(
        @PathVariable UUID deliveryManagerId,
        @Valid @RequestBody UpdateDeliveryManagerRequest request,
        CurrentUser currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            deliveryManagerService.updateDeliveryManager(deliveryManagerId, request, currentUser)
        ));
    }

    @DeleteMapping("/{deliveryManagerId}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable UUID deliveryManagerId,
        CurrentUser currentUser
    ) {
        deliveryManagerService.deleteDeliveryManager(deliveryManagerId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
