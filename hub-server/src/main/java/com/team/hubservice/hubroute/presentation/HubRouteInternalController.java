package com.team.hubservice.hubroute.presentation;

import com.team.hubservice.hubroute.application.HubRouteAiService;
import com.team.hubservice.hubroute.application.HubRouteOptimalService;
import com.team.hubservice.hubroute.application.OptimalRouteQuery;
import com.team.hubservice.hubroute.application.OptimalRouteResult;
import com.team.hubservice.hubroute.presentation.dto.AiRouteResponse;
import com.team.hubservice.hubroute.presentation.dto.OptimalRouteResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/hub-route")
public class HubRouteInternalController {

    private final HubRouteOptimalService hubRouteOptimalService;
    private final HubRouteAiService hubRouteAiService;

    public HubRouteInternalController(HubRouteOptimalService hubRouteOptimalService,
                                      HubRouteAiService hubRouteAiService) {
        this.hubRouteOptimalService = hubRouteOptimalService;
        this.hubRouteAiService = hubRouteAiService;
    }

    @GetMapping("/optimal")
    public ResponseEntity<Map<String, Object>> getOptimalRoute(
        @RequestParam UUID departureHubId,
        @RequestParam UUID arrivalHubId) {

        OptimalRouteQuery query = new OptimalRouteQuery(departureHubId, arrivalHubId);
        OptimalRouteResult result = hubRouteOptimalService.findOptimalRoute(query);
        OptimalRouteResponse response = OptimalRouteResponse.from(result);

        Map<String, Object> body = new HashMap<>();
        body.put("code", HttpStatus.OK.value());
        body.put("message", "허브 최적 경로 조회를 성공했습니다.");
        body.put("data", response);

        return ResponseEntity.ok(body);
    }

    @GetMapping
    public ResponseEntity<AiRouteResponse> getRouteForAi(@RequestParam UUID originId,
                                                         @RequestParam UUID destinationId,
                                                         @RequestHeader(value = "X-Internal-Request", required = true) String internalHeader) {

        if (!"true".equals(internalHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AiRouteResponse response = hubRouteAiService.getRouteInfoForAi(originId, destinationId);

        if (response == null) {
            System.out.println("[HUB-DEBUG] 해당 경로 데이터가 DB에 없습니다.");
            return ResponseEntity.notFound().build();
        }
        System.out.println("[HUB-DEBUG] 데이터 조회 성공: duration=" + response.duration());

        return ResponseEntity.ok(response);
    }
}
