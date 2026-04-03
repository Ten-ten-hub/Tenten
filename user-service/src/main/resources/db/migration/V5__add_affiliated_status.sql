/* =========================================================
   [USER-SERVICE] V5
   - p_user: affiliated_status 컬럼 추가
   ========================================================= */

ALTER TABLE p_user
    ADD COLUMN affiliated_status VARCHAR(20) NOT NULL DEFAULT 'UNAFFILIATED';

ALTER TABLE p_user
    ADD CONSTRAINT chk_p_user_affiliated_status
        CHECK (affiliated_status IN ('UNAFFILIATED', 'HUB_AFFILIATED','COM_AFFILIATED','NOT_APPLICABLE'));
