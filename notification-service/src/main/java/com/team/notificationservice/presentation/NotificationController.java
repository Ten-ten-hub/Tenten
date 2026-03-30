package com.team.notificationservice.presentation;

import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSearchCondition;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.presentation.common.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/slack")
    public ApiResponse<String> send(@RequestBody NotificationRequest request) {
        log.info("알림 서비스 요청 수신: {}", request); // 요청이 들어오는지 확인
        notificationService.createAndSend(request);
        return ApiResponse.success("OK");
    }

    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getNotification(@PathVariable UUID id) {
        return ApiResponse.success(notificationService.getNotification(id));
    }

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
        NotificationSearchCondition condition,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(notificationService.searchNotifications(condition, pageable));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(
        @PathVariable UUID id,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 헤더값이 없으면 기본값 "SYSTEM" 사용
        String deletedBy = (userId != null) ? userId : "SYSTEM";

        notificationService.deleteNotification(id, deletedBy);

        return ApiResponse.success(null);
    }
}
