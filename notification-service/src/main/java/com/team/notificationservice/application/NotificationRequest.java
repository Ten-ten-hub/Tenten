package com.team.notificationservice.application;

import com.team.notificationservice.domain.MsgType;
import java.util.UUID;

public record NotificationRequest(
    String receiverSlackId,
    String email,
    UUID orderId,
    String message,
    MsgType msgType
) {
}
