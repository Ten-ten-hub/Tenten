/* =========================================================
   NOTIFICATION-SERVICE : V1__init.sql
   - p_notification 생성
   주의:
   - 타 서비스(order)에는 물리 FK를 걸지 않음 (Logical FK)
   ========================================================= */

-- =========================================================
-- TABLE: p_notification
-- =========================================================
CREATE TABLE p_notification
(
    id                UUID PRIMARY KEY,
    order_id          UUID,   -- logical FK -> p_order.id
    receiver_id       UUID, -- logical FK -> p_user.id
    receiver_slack_id VARCHAR(255),
    msg_type          VARCHAR(50) NOT NULL,
    msg_content       TEXT        NOT NULL,
    send_status       VARCHAR(50) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by        VARCHAR(255),
    updated_at        TIMESTAMP WITH TIME ZONE,
    updated_by        VARCHAR(255),
    deleted_at        TIMESTAMP WITH TIME ZONE,
    deleted_by        VARCHAR(255)
);

-- =========================================================
-- CONSTRAINT / INDEX
-- =========================================================
-- 알림 조회 시 성능을 위해 인덱스 추가 (특히 삭제되지 않은 데이터 조회용)
CREATE INDEX idx_notification_order_id ON p_notification (order_id);
CREATE INDEX idx_notification_receiver_slack_id ON p_notification (receiver_slack_id);
CREATE INDEX idx_notification_deleted_at ON p_notification (deleted_at);
