package com.team.notificationservice.presentation;

import com.team.notificationservice.application.AiNotificationRequest;
import com.team.notificationservice.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/internal")
@RequiredArgsConstructor
public class InternalNotificationController {
    private final NotificationService notificationService;

    @PostMapping("/ai-slack")
    public void createAiNotification(@RequestBody AiNotificationRequest request) {
        notificationService.createWithAiAnalysis(request, "ORDER_ALERT");
    }
}
