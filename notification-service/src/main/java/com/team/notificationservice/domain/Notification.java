package com.team.notificationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String receiverSlackId;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private MsgType msgType;

    @Column(columnDefinition = "TEXT")
    private String msgContent;

    @Enumerated(EnumType.STRING)
    private SendStatus sendStatus;

    public void markAsSuccess() {
        this.sendStatus = SendStatus.SUCCESS;
    }

    public void markAsFailed() {
        this.sendStatus = SendStatus.FAIL;
    }

    public void delete(String adminId) {
        super.delete(adminId);
//        this.sendStatus = SendStatus.CANCEL;
    }
}