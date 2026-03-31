package com.team.hubservice.hub.api;

import com.team.hubservice.hub.application.HubService;
import com.team.hubservice.hub.dto.HubCreateRequest;
import com.team.hubservice.hub.dto.HubResponse;
import com.team.hubservice.hub.dto.HubUpdateRequest;
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
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Map<String, Object>> createHub(@Valid @RequestBody HubCreateRequest request) {
        HubResponse response = hubService.createHub(request);
        return buildResponse(HttpStatus.CREATED.value(), "허브 생성이 완료되었습니다.", response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHubs(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        int validSize = (size == 10 || size == 30 || size == 50) ? size : 10;

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), validSize);
        Page<HubResponse> hubPage = hubService.getHubs(name, pageable);

        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("currentPage", hubPage.getNumber() + 1);
        pageInfo.put("size", hubPage.getSize());
        pageInfo.put("totalElements", hubPage.getTotalElements());
        pageInfo.put("totalPages", hubPage.getTotalPages());

        Map<String, Object> data = new HashMap<>();
        data.put("content", hubPage.getContent());
        data.put("pageInfo", pageInfo);

        return buildResponse(HttpStatus.OK.value(), "허브 목록 조회를 성공했습니다.", data);
    }

    @GetMapping("/{hubId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHub(@PathVariable UUID hubId) {
        HubResponse response = hubService.getHub(hubId);
        return buildResponse(HttpStatus.OK.value(), "허브 단건 조회를 성공했습니다.", response);
    }

    @PatchMapping("/{hubId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Map<String, Object>> updateHub(
            @PathVariable UUID hubId,
            @Valid @RequestBody HubUpdateRequest request) {

        HubResponse response = hubService.updateHub(hubId, request);
        return buildResponse(HttpStatus.OK.value(), "허브 정보가 성공적으로 수정되었습니다.", response);
    }

    @DeleteMapping("/{hubId}")
    @PreAuthorize("hasRole('MASTER')")
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
