package com.team.notificationservice.domain;


import com.team.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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

    @Column(name = "receiver_id")
    private UUID receiverId;

    @Column(name = "receiver_slack_id", nullable = false, length = 100)
    private String receiverSlackId;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "msg_type", nullable = false, length = 50)
    private MsgType msgType;

    @Column(name = "msg_content", columnDefinition = "TEXT", nullable = false)
    private String msgContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false, length = 20)
    @Builder.Default
    private SendStatus sendStatus = SendStatus.PENDING;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "ref_id")
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
