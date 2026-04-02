package com.team.notificationservice.domain;


import com.team.common.BaseEntity;
import com.team.notificationservice.presentation.common.ErrorCode;
import com.team.notificationservice.presentation.common.ServiceException;
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

    @Column(nullable = false, length = 100)
    private String receiverSlackId;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MsgType msgType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String msgContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SendStatus sendStatus = SendStatus.PENDING;

    private UUID refId;

    public void markAsSuccess() {
        this.sendStatus = SendStatus.SUCCESS;
    }

    public void markAsFailed() {
        this.sendStatus = SendStatus.FAIL;
    }

    public void delete(String adminId) {
        try {
            // adminId가 유효한 UUID 형식인지 확인 후 부모 메서드 호출
            super.softDelete(UUID.fromString(adminId));
            //this.sendStatus = SendStatus.CANCEL;
        } catch (IllegalArgumentException | NullPointerException e) {
            // UUID 형식이 아닐 경우에 대한 에러 처리
            throw new ServiceException(ErrorCode.COMMON_INVALID_USER_ID);
        }
    }
}
