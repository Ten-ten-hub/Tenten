package com.team.notificationservice.application;

public record NotificationSearchCondition(
    String slackId,
    String keyword
) {
}
