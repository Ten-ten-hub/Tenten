/* =========================================================
   COMPANY-SERVICE : V1__init.sql
   - enum 생성
   - p_company 생성
   - 인덱스 / 유니크 제약조건 생성
   주의:
   - 타 서비스 테이블(p_hub 등)에는 물리 FK를 걸지 않음
   - MSA 구조상 hub_id는 논리 FK로만 관리
   ========================================================= */

-- =========================================================
-- ENUM TYPE
-- =========================================================
CREATE TYPE company_type AS ENUM (
    'PRODUCER',
    'RECEIVER'
);

-- =========================================================
-- TABLE: p_company
-- =========================================================
CREATE TABLE p_company (
                           id UUID PRIMARY KEY,
                           name VARCHAR(150) NOT NULL,
                           company_type company_type NOT NULL,
                           hub_id UUID NOT NULL, -- logical FK -> p_hub.id
                           address VARCHAR(255) NOT NULL,
                           address_detail VARCHAR(255),
                           zipcode VARCHAR(20),
                           contact_name VARCHAR(100),
                           contact_phone VARCHAR(30),
                           contact_slack_id VARCHAR(100),
                           is_active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                           created_by UUID NOT NULL,
                           updated_at TIMESTAMP,
                           updated_by UUID,
                           deleted_at TIMESTAMP,
                           deleted_by UUID
);

-- =========================================================
-- CONSTRAINT / INDEX
-- =========================================================
ALTER TABLE p_company
    ADD CONSTRAINT uk_p_company_hub_id_name UNIQUE (hub_id, name);

CREATE INDEX idx_p_company_hub_id ON p_company (hub_id);
CREATE INDEX idx_p_company_deleted_at ON p_company (deleted_at);
CREATE INDEX idx_p_company_name ON p_company (name);