/* =========================================================
   [USER-SERVICE] V2
   - role, signup_status: native ENUM -> VARCHAR
   - email, phone_number: NOT NULL 적용
   ========================================================= */

-- role 컬럼: user_role ENUM -> VARCHAR(50)
ALTER TABLE p_user
    ALTER COLUMN role TYPE VARCHAR(50) USING role::TEXT;

-- signup_status 컬럼: signup_status ENUM -> VARCHAR(20)
ALTER TABLE p_user
    ALTER COLUMN signup_status TYPE VARCHAR(20) USING signup_status::TEXT;

-- native ENUM 타입 제거
DROP TYPE user_role CASCADE;
DROP TYPE signup_status CASCADE;

-- CHECK 제약으로 값 집합 보존
ALTER TABLE p_user
    ADD CONSTRAINT chk_p_user_role
        CHECK (role IN ('HUB_ADMIN', 'MASTER_ADMIN', 'HUB_DELIVERY_MANAGER', 'COM_DELIVERY_MANAGER', 'COMPANY_MANAGER'));

ALTER TABLE p_user
    ADD CONSTRAINT chk_p_user_signup_status
        CHECK (signup_status IN ('PENDING', 'APPROVED', 'REJECTED', 'INACTIVE'));

-- NULL 사전 검사: NULL 데이터가 존재하면 명확한 오류 메시지로 실패
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM p_user WHERE email IS NULL) THEN
        RAISE EXCEPTION 'Migration V2 failed: p_user.email 에 NULL 값이 존재합니다. 데이터를 정리한 후 재실행하세요.';
    END IF;
    IF EXISTS (SELECT 1 FROM p_user WHERE phone_number IS NULL) THEN
        RAISE EXCEPTION 'Migration V2 failed: p_user.phone_number 에 NULL 값이 존재합니다. 데이터를 정리한 후 재실행하세요.';
    END IF;
END $$;

-- email NOT NULL 적용
ALTER TABLE p_user
    ALTER COLUMN email SET NOT NULL;

-- phone_number NOT NULL 적용
ALTER TABLE p_user
    ALTER COLUMN phone_number SET NOT NULL;
