/* =========================================================
   [USER-SERVICE] V3
   - created_by, updated_by, deleted_by: VARCHAR -> UUID
   - id, user_id, hub_id, company_id: VARCHAR -> UUID
   ========================================================= */

-- =========================================================
-- p_company_user FK 제거 후 UUID 변환
-- =========================================================
ALTER TABLE p_company_user DROP CONSTRAINT fk_p_company_user_user;
ALTER TABLE p_hub_user     DROP CONSTRAINT fk_p_hub_user_user;

-- =========================================================
-- TABLE: p_user
-- =========================================================
ALTER TABLE p_user
    ALTER COLUMN id           TYPE UUID USING id::UUID,
    ALTER COLUMN created_by   TYPE UUID USING created_by::UUID,
    ALTER COLUMN updated_by   TYPE UUID USING updated_by::UUID,
    ALTER COLUMN deleted_by   TYPE UUID USING deleted_by::UUID;

-- =========================================================
-- TABLE: p_hub_user
-- =========================================================
ALTER TABLE p_hub_user
    ALTER COLUMN id           TYPE UUID USING id::UUID,
    ALTER COLUMN user_id      TYPE UUID USING user_id::UUID,
    ALTER COLUMN hub_id       TYPE UUID USING hub_id::UUID,
    ALTER COLUMN created_by   TYPE UUID USING created_by::UUID,
    ALTER COLUMN updated_by   TYPE UUID USING updated_by::UUID,
    ALTER COLUMN deleted_by   TYPE UUID USING deleted_by::UUID;

-- =========================================================
-- TABLE: p_company_user
-- =========================================================
ALTER TABLE p_company_user
    ALTER COLUMN id           TYPE UUID USING id::UUID,
    ALTER COLUMN user_id      TYPE UUID USING user_id::UUID,
    ALTER COLUMN company_id   TYPE UUID USING company_id::UUID,
    ALTER COLUMN created_by   TYPE UUID USING created_by::UUID,
    ALTER COLUMN updated_by   TYPE UUID USING updated_by::UUID,
    ALTER COLUMN deleted_by   TYPE UUID USING deleted_by::UUID;

-- =========================================================
-- FK 복원
-- =========================================================
ALTER TABLE p_hub_user
    ADD CONSTRAINT fk_p_hub_user_user
        FOREIGN KEY (user_id) REFERENCES p_user(id);

ALTER TABLE p_company_user
    ADD CONSTRAINT fk_p_company_user_user
        FOREIGN KEY (user_id) REFERENCES p_user(id);
