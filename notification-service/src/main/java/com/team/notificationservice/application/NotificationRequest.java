package com.team.notificationservice.application;

import com.team.notificationservice.domain.MsgType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record NotificationRequest(
    String receiverSlackId,
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,
    UUID orderId,
    @NotBlank(message = "메시지 내용은 필수입니다.")
    String message,
    @NotNull(message = "메시지 타입은 필수입니다.")
    MsgType msgType
) {
}
