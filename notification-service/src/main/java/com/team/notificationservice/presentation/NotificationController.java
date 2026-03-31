package com.team.notificationservice.presentation;

import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSearchCondition;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.presentation.common.ApiResponse;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 외부용 API
    @PostMapping("/api/v1/notifications/slack")
    public ApiResponse<String> send(@RequestBody @Valid NotificationRequest request) {
        notificationService.createAndSend(request);
        return ApiResponse.success("OK");
    }

    // 내부 시스템 호출용 (게이트웨이 설정 없이 서비스명:8085/internal/v1/... 으로 직접 호출)
    @PostMapping("/internal/v1/notifications/slack")
    public ApiResponse<String> internalSend(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                            @RequestBody @Valid NotificationRequest request) {

        // 내부 호출 인증 체크 (예시?)
        if (token == null || !token.equals("INTERNAL_SECRET")) {
            throw new ServiceException(ErrorCode.COMMON_INVALID_INPUT_VALUE); // notification 권한 에러 코드로 대체하기
        }

        notificationService.createAndSend(request);
        return ApiResponse.success("OK");
    }

    @GetMapping("/api/v1/notifications/{id}")
    public ApiResponse<NotificationResponse> getNotification(@PathVariable UUID id) {
        return ApiResponse.success(notificationService.getNotification(id));
    }

    @GetMapping("/api/v1/notifications")
    public ApiResponse<Page<NotificationResponse>> getNotifications(
        @Valid NotificationSearchCondition condition,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(notificationService.searchNotifications(condition, pageable));
    }

    @DeleteMapping("/api/v1/notifications/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID id,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String deletedBy = (userId != null) ? userId : "SYSTEM";
        notificationService.deleteNotification(id, deletedBy);

        return ResponseEntity.noContent().build();
    }
}
