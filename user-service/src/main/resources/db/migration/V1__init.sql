/* =========================================================
   [USER-SERVICE]
   Tables:
   - p_user
   - p_hub_user
   - p_company_user
   ========================================================= */

-- =========================================================
-- ENUM TYPE
-- =========================================================
CREATE TYPE user_role AS ENUM (
    'HUB_ADMIN',
    'MASTER_ADMIN',
    'HUB_DELIVERY_MANAGER',
    'COM_DELIVERY_MANAGER',
    'COMPANY_MANAGER'
    );

CREATE TYPE signup_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'INACTIVE'
    );

-- =========================================================
-- TABLE: p_user
-- =========================================================
CREATE TABLE p_user (
                        id UUID PRIMARY KEY,
                        login_id VARCHAR(50) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        role user_role NOT NULL,
                        slack_id VARCHAR(100) NOT NULL UNIQUE,
                        email VARCHAR(255) UNIQUE,
                        phone_number VARCHAR(20),
                        signup_status signup_status NOT NULL DEFAULT 'PENDING',
                        last_login_at TIMESTAMP,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        created_by UUID NOT NULL,
                        updated_at TIMESTAMP,
                        updated_by UUID,
                        deleted_at TIMESTAMP,
                        deleted_by UUID
);

CREATE INDEX idx_p_user_name ON p_user (name);
CREATE INDEX idx_p_user_role ON p_user (role);
CREATE INDEX idx_p_user_signup_status ON p_user (signup_status);
CREATE INDEX idx_p_user_deleted_at ON p_user (deleted_at);

-- =========================================================
-- TABLE: p_hub_user
-- user-service 내부에서 user_id만 물리 FK
-- hub_id는 hub-service 소속이므로 논리 FK
-- =========================================================
CREATE TABLE p_hub_user (
                            id UUID PRIMARY KEY,
                            user_id UUID NOT NULL,
                            hub_id UUID NOT NULL, -- logical FK -> p_hub.id
                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            created_by UUID NOT NULL,
                            updated_at TIMESTAMP,
                            updated_by UUID,
                            deleted_at TIMESTAMP,
                            deleted_by UUID,
                            CONSTRAINT fk_p_hub_user_user
                                FOREIGN KEY (user_id) REFERENCES p_user(id)
);

CREATE INDEX idx_p_hub_user_user_id ON p_hub_user (user_id);
CREATE INDEX idx_p_hub_user_hub_id ON p_hub_user (hub_id);
CREATE INDEX idx_p_hub_user_deleted_at ON p_hub_user (deleted_at);

-- =========================================================
-- TABLE: p_company_user
-- user-service 내부에서 user_id만 물리 FK
-- company_id는 company-service 소속이므로 논리 FK
-- =========================================================
CREATE TABLE p_company_user (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL,
                                company_id UUID NOT NULL, -- logical FK -> p_company.id
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                created_by UUID NOT NULL,
                                updated_at TIMESTAMP,
                                updated_by UUID,
                                deleted_at TIMESTAMP,
                                deleted_by UUID,
                                CONSTRAINT fk_p_company_user_user
                                    FOREIGN KEY (user_id) REFERENCES p_user(id)
);

CREATE INDEX idx_p_company_user_user_id ON p_company_user (user_id);
CREATE INDEX idx_p_company_user_company_id ON p_company_user (company_id);
CREATE INDEX idx_p_company_user_deleted_at ON p_company_user (deleted_at);