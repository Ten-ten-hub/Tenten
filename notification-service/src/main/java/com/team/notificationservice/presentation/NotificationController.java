package com.team.notificationservice.presentation;

import com.team.common.page.PageResponse;
import com.team.common.page.PageSizeUtils;
import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSearchCondition;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.presentation.common.ApiResponse;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    @Value("${internal.auth.token:}") // application.yml 미설정 시 빈 값 주입
    private String internalAuthToken;

    // 외부용 API
    @PostMapping("/api/v1/notifications/slack")
    public ApiResponse<String> send(@RequestHeader(value = "X-User-Id", required = false) String userId, // 헤더 추가
                                    @RequestBody @Valid NotificationRequest request) {
        notificationService.createAndSend(request, userId);
        return ApiResponse.success("OK");
    }

    // 내부 시스템 호출용 (게이트웨이 설정 없이 서비스명:8085/internal/v1/... 으로 직접 호출)
    @PostMapping("/internal/v1/notifications/slack")
    public ApiResponse<String> internalSend(
        @RequestHeader(value = "X-Internal-Token", required = false) String token,
        @RequestBody @Valid NotificationRequest request) {

        // 1. 서버 설정 체크 (5xx)
        if (internalAuthToken == null || internalAuthToken.isBlank()) {
            log.error("Internal Auth Token is not configured in server.");
            throw new ServiceException(ErrorCode.SERVER_CONFIG_ERROR);
        }

        // 2. 타이밍 공격 방지 및 null-safe 비교 (401)
        if (token == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), internalAuthToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ServiceException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // 내부 시스템 호출 시에는 별도 유저 ID가 없을 수 있으므로 null 전달
        notificationService.createAndSend(request, null);
        return ApiResponse.success("OK");
    }

    @GetMapping("/api/v1/notifications/{id}")
    public ApiResponse<NotificationResponse> getNotification(@PathVariable UUID id) {
        return ApiResponse.success(notificationService.getNotification(id));
    }

    @GetMapping("/api/v1/notifications")
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
        @Valid NotificationSearchCondition condition,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        int normalizedSize = PageSizeUtils.normalize(pageable.getPageSize());
        Pageable normalizedPageable = PageRequest.of(
            pageable.getPageNumber(),
            normalizedSize,
            pageable.getSort()
        );

        Page<NotificationResponse> resultPage = notificationService.searchNotifications(condition, normalizedPageable);
        return ApiResponse.success(PageResponse.from(resultPage));
    }

    @DeleteMapping("/api/v1/notifications/{id}")
    public ApiResponse<Void> delete(
        @PathVariable UUID id,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 빈 문자열이나 공백도 SYSTEM으로 정규화
        String deletedBy = (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
        notificationService.deleteNotification(id, deletedBy);

        return ApiResponse.success(null);
    }
}
