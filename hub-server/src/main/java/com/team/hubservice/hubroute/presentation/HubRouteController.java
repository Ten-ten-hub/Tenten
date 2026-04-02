package com.team.hubservice.hubroute.presentation;

import com.team.hubservice.hubroute.application.HubRouteCreateCommand;
import com.team.hubservice.hubroute.application.HubRouteResult;
import com.team.hubservice.hubroute.application.HubRouteService;
import com.team.hubservice.hubroute.application.HubRouteUpdateCommand;
import com.team.hubservice.hubroute.presentation.dto.HubRouteCreateRequest;
import com.team.hubservice.hubroute.presentation.dto.HubRouteResponse;
import com.team.hubservice.hubroute.presentation.dto.HubRouteUpdateRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hub-routes")
public class HubRouteController {

    private final HubRouteService hubRouteService;

    public HubRouteController(HubRouteService hubRouteService) {
        this.hubRouteService = hubRouteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Map<String, Object>> createHubRoute(@Valid @RequestBody HubRouteCreateRequest request) {
        HubRouteCreateCommand command = new HubRouteCreateCommand(
            request.departureHubId(), request.arrivalHubId(), request.duration(), request.distance()
        );
        HubRouteResult result = hubRouteService.createHubRoute(command);
        return buildResponse(HttpStatus.CREATED.value(), "허브 이동 경로 생성이 완료되었습니다.", HubRouteResponse.from(result));
    }

    @GetMapping("/{routeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHubRoute(@PathVariable UUID routeId) {
        HubRouteResult result = hubRouteService.getHubRoute(routeId);
        return buildResponse(HttpStatus.OK.value(), "허브 이동 경로 조회를 성공했습니다.", HubRouteResponse.from(result));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHubRoutes(
        @RequestParam(required = false) UUID departureHubId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {

        int validSize = (size == 10 || size == 30 || size == 50) ? size : 10;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), validSize);

        Page<HubRouteResult> resultPage = hubRouteService.getHubRoutes(departureHubId, pageable);
        Page<HubRouteResponse> routePage = resultPage.map(HubRouteResponse::from);

        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("currentPage", routePage.getNumber() + 1);
        pageInfo.put("size", routePage.getSize());
        pageInfo.put("totalElements", routePage.getTotalElements());
        pageInfo.put("totalPages", routePage.getTotalPages());

        Map<String, Object> data = new HashMap<>();
        data.put("content", routePage.getContent());
        data.put("pageInfo", pageInfo);

        return buildResponse(HttpStatus.OK.value(), "허브 이동 경로 목록 조회를 성공했습니다.", data);
    }

    @PatchMapping("/{routeId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Map<String, Object>> updateHubRoute(
        @PathVariable UUID routeId,
        @Valid @RequestBody HubRouteUpdateRequest request) {
        HubRouteUpdateCommand command = new HubRouteUpdateCommand(request.duration(), request.distance());
        HubRouteResult result = hubRouteService.updateHubRoute(routeId, command);
        return buildResponse(HttpStatus.OK.value(), "허브 이동 경로가 성공적으로 수정되었습니다.", HubRouteResponse.from(result));
    }

    @DeleteMapping("/{routeId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Map<String, Object>> deleteHubRoute(
        @PathVariable UUID routeId,
        @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        hubRouteService.deleteHubRoute(routeId, userId);
        return buildResponse(HttpStatus.OK.value(), "허브 이동 경로가 성공적으로 삭제 처리되었습니다.", null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int code, String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        return ResponseEntity.status(code).body(body);
    }
}
