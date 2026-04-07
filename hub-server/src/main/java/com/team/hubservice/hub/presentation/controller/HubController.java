package com.team.hubservice.hub.presentation.controller;

import com.team.common.page.PageResponse;
import com.team.hubservice.hub.application.dto.HubCreateCommand;
import com.team.hubservice.hub.application.dto.HubResult;
import com.team.hubservice.hub.application.service.HubService;
import com.team.hubservice.hub.application.dto.HubUpdateCommand;
import com.team.common.page.PageSizeUtils;
import com.team.hubservice.hub.presentation.dto.HubCreateRequest;
import com.team.hubservice.hub.presentation.dto.HubResponse;
import com.team.hubservice.hub.presentation.dto.HubUpdateRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hubs")
public class HubController {

    private final HubService hubService;

    public HubController(HubService hubService) {
        this.hubService = hubService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createHub(@Valid @RequestBody HubCreateRequest request) {
        HubCreateCommand command = new HubCreateCommand(
            request.name(),
            request.address(),
            request.latitude(),
            request.longitude()
        );

        HubResult result = hubService.createHub(command);

        HubResponse response = HubResponse.from(result);

        return buildResponse(HttpStatus.CREATED.value(), "허브 생성이 완료되었습니다.", response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHubs(
        @RequestParam(required = false) String name,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {

        int validSize = PageSizeUtils.normalize(size);

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), validSize);
        Page<HubResult> resultPage = hubService.getHubs(name, pageable);

        Page<HubResponse> hubPage = resultPage.map(HubResponse::from);

        return buildResponse(HttpStatus.OK.value(), "허브 목록 조회를 성공했습니다.", PageResponse.from(hubPage));
    }

    @GetMapping("/{hubId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHub(@PathVariable UUID hubId) {
        HubResult result = hubService.getHub(hubId);
        HubResponse response = HubResponse.from(result);
        return buildResponse(HttpStatus.OK.value(), "허브 단건 조회를 성공했습니다.", response);
    }

    @PatchMapping("/{hubId}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateHub(
            @PathVariable UUID hubId,
            @Valid @RequestBody HubUpdateRequest request) {

        HubUpdateCommand command = new HubUpdateCommand(
            request.name(),
            request.latitude(),
            request.longitude()
        );

        HubResult result = hubService.updateHub(hubId, command);

        HubResponse response = HubResponse.from(result);
        return buildResponse(HttpStatus.OK.value(), "허브 정보가 성공적으로 수정되었습니다.", response);
    }

    @DeleteMapping("/{hubId}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteHub(
            @PathVariable UUID hubId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        hubService.deleteHub(hubId, userId);
        return buildResponse(HttpStatus.OK.value(), "허브가 성공적으로 삭제 처리되었습니다.", null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int code, String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        return ResponseEntity.status(code).body(body);
    }
}
