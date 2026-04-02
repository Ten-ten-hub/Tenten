/* =========================================================
   [USER-SERVICE] V4
   - chk_p_user_role: NONE 추가
   ========================================================= */

ALTER TABLE p_user DROP CONSTRAINT chk_p_user_role;

ALTER TABLE p_user
    ADD CONSTRAINT chk_p_user_role
        CHECK (role IN ('NONE', 'HUB_ADMIN', 'MASTER_ADMIN', 'HUB_DELIVERY_MANAGER', 'COM_DELIVERY_MANAGER', 'COMPANY_MANAGER'));
