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

-- email NOT NULL 적용
ALTER TABLE p_user
    ALTER COLUMN email SET NOT NULL;

-- phone_number NOT NULL 적용
ALTER TABLE p_user
    ALTER COLUMN phone_number SET NOT NULL;
