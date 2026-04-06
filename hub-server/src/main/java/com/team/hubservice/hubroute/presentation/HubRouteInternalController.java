package com.team.hubservice.hubroute.presentation;

import com.team.hubservice.hubroute.application.HubRouteAiService;
import com.team.hubservice.hubroute.application.HubRouteOptimalService;
import com.team.hubservice.hubroute.application.HubRouteTmapSyncService;
import com.team.hubservice.hubroute.application.OptimalRouteQuery;
import com.team.hubservice.hubroute.application.OptimalRouteResult;
import com.team.hubservice.hubroute.application.TmapHubRouteBulkSyncResult;
import com.team.hubservice.hubroute.application.TmapHubRouteSyncResult;
import com.team.hubservice.hubroute.presentation.dto.AiRouteResponse;
import com.team.hubservice.hubroute.presentation.dto.OptimalRouteResponse;
import com.team.hubservice.hubroute.presentation.dto.TmapHubRouteBulkSyncResponse;
import com.team.hubservice.hubroute.presentation.dto.TmapHubRouteSyncResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/hub-route")
public class HubRouteInternalController {

    private final HubRouteOptimalService hubRouteOptimalService;
    private final HubRouteAiService hubRouteAiService;
    private final HubRouteTmapSyncService hubRouteTmapSyncService;

    public HubRouteInternalController(
        HubRouteOptimalService hubRouteOptimalService,
        HubRouteAiService hubRouteAiService,
        HubRouteTmapSyncService hubRouteTmapSyncService
    ) {
        this.hubRouteOptimalService = hubRouteOptimalService;
        this.hubRouteAiService = hubRouteAiService;
        this.hubRouteTmapSyncService = hubRouteTmapSyncService;
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRouteForAi(
        @RequestParam UUID originId,
        @RequestParam UUID destinationId,
        @RequestHeader(value = "X-Internal-Request", required = true) String internalHeader) {

        if (!"true".equals(internalHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AiRouteResponse response = hubRouteAiService.getRouteInfoForAi(originId, destinationId);

        Map<String, Object> body = new HashMap<>();
        body.put("code", HttpStatus.OK.value());
        body.put("message", "AI 학습용 단건 허브 경로 조회를 성공했습니다.");
        body.put("data", response);

        return ResponseEntity.ok(body);
    }

    /**
     * 티맵 도로 경로 기준으로 허브 간 duration(분)·distance(km)를 적재합니다.
     * {@code departureHubId}, {@code arrivalHubId} 가 모두 있으면 해당 유향 1건만 동기화하고, 없으면 전 허브 유향 쌍 전체를 동기화합니다.
     */
    @PostMapping("/sync-from-tmap")
    public ResponseEntity<Map<String, Object>> syncRoutesFromTmap(
        @RequestParam(required = false) UUID departureHubId,
        @RequestParam(required = false) UUID arrivalHubId,
        @RequestHeader(value = "X-Internal-Request", required = true) String internalHeader) {

        if (!"true".equals(internalHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> body = new HashMap<>();
        if (departureHubId != null ^ arrivalHubId != null) {
            body.put("code", HttpStatus.BAD_REQUEST.value());
            body.put("message", "departureHubId 와 arrivalHubId 는 둘 다 지정하거나 둘 다 생략해야 합니다.");
            return ResponseEntity.badRequest().body(body);
        }
        if (departureHubId != null && arrivalHubId != null) {
            TmapHubRouteSyncResult result = hubRouteTmapSyncService.syncOnePair(departureHubId, arrivalHubId);
            body.put("code", HttpStatus.OK.value());
            body.put("message", "티맵 기준 허브 경로 1건 동기화를 완료했습니다.");
            body.put("data", TmapHubRouteSyncResponse.from(result));
            return ResponseEntity.ok(body);
        }

        TmapHubRouteBulkSyncResult bulk = hubRouteTmapSyncService.syncAllDirectedPairs();
        body.put("code", HttpStatus.OK.value());
        body.put("message", "티맵 기준 허브 경로 전체 동기화 배치를 완료했습니다.");
        body.put("data", TmapHubRouteBulkSyncResponse.from(bulk));
        return ResponseEntity.ok(body);
    }
}
