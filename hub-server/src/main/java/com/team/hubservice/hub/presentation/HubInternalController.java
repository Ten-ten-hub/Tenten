package com.team.hubservice.hub.presentation;

import com.team.hubservice.hub.application.HubService;
import com.team.hubservice.hub.presentation.dto.HubInternalResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<Map<String, Object>> checkHubExists(@PathVariable UUID hubId) {
        boolean isExists = hubService.checkHubExists(hubId);

        Map<String, Object> data = new HashMap<>();
        data.put("hubId", hubId.toString());
        data.put("exists", isExists);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("code", "SUCCESS");
        response.put("message", "요청이 성공했습니다.");

        return ResponseEntity.ok(response);
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
