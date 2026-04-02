package com.team.notificationservice.domain;


import com.team.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "p_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID receiverId;

    @Column(nullable = false, length = 100)
    private String receiverSlackId;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MsgType msgType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String msgContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SendStatus sendStatus = SendStatus.PENDING;

    private UUID refId;

    public void markAsSuccess() {
        this.sendStatus = SendStatus.SUCCESS;
    }

    public void markAsFailed() {
        this.sendStatus = SendStatus.FAIL;
    }

    public void delete(UUID adminId) {
        super.softDelete(adminId);
    }
}
