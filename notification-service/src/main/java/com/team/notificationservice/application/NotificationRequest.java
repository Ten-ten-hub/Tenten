package com.team.notificationservice.application;

import com.team.notificationservice.domain.MsgType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {
    private String receiverSlackId;
    private String email;
    private UUID orderId;
    private String message;
    private MsgType msgType;
}