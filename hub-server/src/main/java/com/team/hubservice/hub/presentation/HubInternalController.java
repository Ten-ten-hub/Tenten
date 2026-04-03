package com.team.hubservice.hub.presentation;

import com.team.hubservice.hub.application.HubService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/hubs")
public class HubInternalController {

    private final HubService hubService;

    public HubInternalController(HubService hubService) {
        this.hubService = hubService;
    }

    @GetMapping("/{hubId}/exists")
    public ResponseEntity<Map<String, Object>> checkHubExists(
        @PathVariable UUID hubId,
        @RequestHeader(value = "X-Internal-Request", required = true) String internalHeader) {

        if (!"true".equals(internalHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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

    // 임시
    @GetMapping("/{hubId}/exists/v2")
    public ResponseEntity<Void> checkHubExistsV2(@PathVariable UUID hubId) {
        boolean exists = hubService.checkHubExists(hubId);
        if (!exists) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }
}
