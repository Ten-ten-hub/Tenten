package com.team.notificationservice.presentation;

import com.team.notificationservice.application.NotificationRequest;
import com.team.notificationservice.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/slack")
    public ResponseEntity<String> send(@RequestBody NotificationRequest request) {
        log.info("알림 서비스 요청 수신: {}", request); // 요청이 들어오는지 확인
        notificationService.createAndSend(request);
        return ResponseEntity.ok("OK");
    }
}