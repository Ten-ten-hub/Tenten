package com.team.hubservice.hub.presentation.controller;

import com.team.common.ApiResponse;
import com.team.hubservice.hub.application.service.HubService;
import com.team.hubservice.hub.presentation.dto.HubExistsPayload;
import com.team.hubservice.hub.presentation.dto.HubInternalResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/hubs")
public class HubInternalController {

    private final HubService hubService;

    public HubInternalController(HubService hubService) {
        this.hubService = hubService;
    }

    @GetMapping("/{hubId}/exists")
    public ResponseEntity<ApiResponse<HubExistsPayload>> checkHubExists(@PathVariable UUID hubId) {
        boolean isExists = hubService.checkHubExists(hubId);
        return ResponseEntity.ok(ApiResponse.success(new HubExistsPayload(hubId, isExists)));
    }

    @GetMapping("/{hubId}/exists/v2")
    public ResponseEntity<Void> checkHubExistsV2(@PathVariable UUID hubId) {
        boolean exists = hubService.checkHubExists(hubId);
        if (!exists) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<HubInternalResponse> getHubInternal(@PathVariable UUID hubId) {
        var hub = hubService.getHubInternal(hubId);

        return ResponseEntity.ok(new HubInternalResponse(
            hub.id(),
            hub.name(),
            hub.address()
        ));
    }
}
