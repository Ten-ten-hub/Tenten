package com.team.notificationservice.presentation;

import com.team.common.ApiResponse;
import com.team.common.page.PageResponse;
import com.team.common.page.PageSizeUtils;
import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationSearchCondition;
import com.team.notificationservice.application.NotificationService;
import com.team.notificationservice.presentation.common.RequireRole;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // 외부용 API (게이트웨이 통과)
    @PostMapping("/api/v1/notifications/slack")
    public ApiResponse<String> send(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                    @RequestBody @Valid NotificationRequest request) {
        notificationService.createAndSend(request, userId);
        return ApiResponse.success("OK");
    }

    @GetMapping("/api/v1/notifications/{id}")
    @RequireRole({"MASTER_ADMIN"})
    public ApiResponse<NotificationResponse> getNotification(@PathVariable UUID id) {
        return ApiResponse.success(notificationService.getNotification(id));
    }

    @GetMapping("/api/v1/notifications")
    @RequireRole({"MASTER_ADMIN"})
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
        @Valid NotificationSearchCondition condition,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // 1. common 유틸을 사용한 페이지 사이즈 정규화
        int normalizedSize = PageSizeUtils.normalize(pageable.getPageSize());

        // 2. 클라이언트의 1-기반 페이지를 서버의 0-기반 페이지로 변환
        int zeroBasedPage = Math.max(pageable.getPageNumber() - 1, 0);

        // 3. 변환된 페이지 번호로 새로운 Pageable 생성
        Pageable normalizedPageable = PageRequest.of(
            zeroBasedPage,
            normalizedSize,
            pageable.getSort()
        );

        Page<NotificationResponse> resultPage = notificationService.searchNotifications(condition, normalizedPageable);

        // 4. PageResponse.from은 내부적으로 다시 +1을 하여 클라이언트에게 1-기반으로 응답함
        return ApiResponse.success(PageResponse.from(resultPage));
    }

    @DeleteMapping("/api/v1/notifications/{id}")
    @RequireRole({"MASTER_ADMIN"})
    public ApiResponse<Void> delete(
        @PathVariable UUID id,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 빈 문자열이나 공백도 SYSTEM으로 정규화
        String deletedBy = (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
        notificationService.deleteNotification(id, deletedBy);

        return ApiResponse.success(null);
    }
}
