package com.team.hubservice.hubroute.presentation;

import com.team.hubservice.hubroute.application.HubRouteOptimalService;
import com.team.hubservice.hubroute.application.OptimalRouteResult;
import com.team.hubservice.hubroute.presentation.dto.OptimalRouteResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.team.hubservice.hubroute.application.OptimalRouteQuery;

@RestController
@RequestMapping("/internal/v1/hub-route")
public class HubRouteInternalController {

    private final HubRouteOptimalService hubRouteOptimalService;

    public HubRouteInternalController(HubRouteOptimalService hubRouteOptimalService) {
        this.hubRouteOptimalService = hubRouteOptimalService;
    }

    @GetMapping("/optimal")
    public ResponseEntity<Map<String, Object>> getOptimalRoute(
        @RequestParam UUID departureHubId,
        @RequestParam UUID arrivalHubId,
        @RequestHeader(value = "X-Internal-Request", required = true) String internalHeader) {

        if (!"true".equals(internalHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        OptimalRouteQuery query = new OptimalRouteQuery(departureHubId, arrivalHubId);
        OptimalRouteResult result = hubRouteOptimalService.findOptimalRoute(query);
        OptimalRouteResponse response = OptimalRouteResponse.from(result);

        Map<String, Object> body = new HashMap<>();
        body.put("code", HttpStatus.OK.value());
        body.put("message", "허브 최적 경로 조회를 성공했습니다.");
        body.put("data", response);

        return ResponseEntity.ok(body);
    }
}
