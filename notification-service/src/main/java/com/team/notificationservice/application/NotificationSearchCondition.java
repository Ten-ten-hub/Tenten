package com.team.notificationservice.application;

import jakarta.validation.constraints.NotBlank;

public record NotificationSearchCondition(
    @NotBlank(message = "조회할 슬랙 ID는 필수입니다.")
    String slackId,
    String keyword
) {
}
