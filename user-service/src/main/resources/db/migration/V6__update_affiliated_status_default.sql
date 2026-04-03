/* =========================================================
   [USER-SERVICE] V6
   - p_user: 기존 affiliated_status UNAFFILIATED -> NOT_APPLICABLE 업데이트
   - DEFAULT 값 NOT_APPLICABLE 으로 변경
   ========================================================= */

UPDATE p_user
SET affiliated_status = 'NOT_APPLICABLE'
WHERE affiliated_status = 'UNAFFILIATED';

ALTER TABLE p_user
    ALTER COLUMN affiliated_status SET DEFAULT 'NOT_APPLICABLE';