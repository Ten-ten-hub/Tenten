/* =========================================================
   [NOTIFICATION-SERVICE]
   Table: p_notification
   ========================================================= */

-- -- 1. ENUM 타입 생성 (메시지 유형 및 전송 상태)
-- CREATE TYPE notification_msg_type AS ENUM (
--     'ORDER_ALERT',
--     'DAILY_REPORT'
-- );
--
-- CREATE TYPE notification_send_status AS ENUM (
--     'PENDING',
--     'SUCCESS',
--     'FAIL'
-- );

-- 2. TABLE 생성
CREATE TABLE p_notification
(
    -- 기본 키
    id                UUID PRIMARY KEY,

    -- 비즈니스 로직 필드
    receiver_id       UUID,                              -- logical FK -> p_user.id
    receiver_slack_id VARCHAR(100)             NOT NULL,
    order_id          UUID,                              -- logical FK -> p_order.id
    msg_type          VARCHAR(50)              NOT NULL,
    msg_content       TEXT                     NOT NULL,
    send_status       VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    ref_id            UUID,                              -- 참조 ID (추가된 필드)

    -- common.BaseEntity 상속 필드 (Audit)
    created_at        TIMESTAMP                NOT NULL DEFAULT NOW(),
    created_by        UUID                     NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    updated_at        TIMESTAMP,
    updated_by        UUID,
    deleted_at        TIMESTAMP,
    deleted_by        UUID
);

-- 3. INDEX 설정 (조회 성능 최적화)
CREATE INDEX idx_p_notification_receiver_id ON p_notification (receiver_id);
CREATE INDEX idx_p_notification_order_id ON p_notification (order_id);
CREATE INDEX idx_p_notification_msg_type ON p_notification (msg_type);
CREATE INDEX idx_p_notification_send_status ON p_notification (send_status);
CREATE INDEX idx_p_notification_deleted_at ON p_notification (deleted_at);
