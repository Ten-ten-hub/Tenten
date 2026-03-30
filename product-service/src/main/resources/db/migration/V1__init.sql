/* =========================================================
   [PRODUCT-SERVICE]
   Tables:
   - p_product
   - p_stock
   - p_stock_history
   ========================================================= */

-- =========================================================
-- ENUM TYPE
-- =========================================================
CREATE TYPE product_status AS ENUM (
    'ON_SALE',
    'OUT_OF_STOCK',
    'DISCONTINUED'
);

CREATE TYPE stock_status AS ENUM (
    'AVAILABLE',
    'SHORTAGE',
    'SOLD_OUT'
);

CREATE TYPE stock_history_type AS ENUM (
    'INBOUND',
    'OUTBOUND',
    'RESTORE',
    'ADJUSTMENT'
);

-- =========================================================
-- TABLE: p_product
-- company_id, hub_id는 타 서비스 소속이라 논리 FK
-- =========================================================
CREATE TABLE p_product (
                           id UUID PRIMARY KEY,
                           name VARCHAR(150) NOT NULL,
                           company_id UUID NOT NULL, -- logical FK -> p_company.id
                           hub_id UUID NOT NULL, -- logical FK -> p_hub.id
                           status product_status NOT NULL DEFAULT 'ON_SALE',
                           unit_price NUMERIC(12,2) NOT NULL,
                           description VARCHAR(500),
                           created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                           created_by UUID NOT NULL,
                           updated_at TIMESTAMP,
                           updated_by UUID,
                           deleted_at TIMESTAMP,
                           deleted_by UUID,
                           CONSTRAINT uk_p_product_company_id_name UNIQUE (company_id, name)
);

CREATE INDEX idx_p_product_company_id ON p_product (company_id);
CREATE INDEX idx_p_product_hub_id ON p_product (hub_id);
CREATE INDEX idx_p_product_status ON p_product (status);
CREATE INDEX idx_p_product_deleted_at ON p_product (deleted_at);

-- =========================================================
-- TABLE: p_stock
-- 전제: 상품 1개는 하나의 허브에만 소속
-- 따라서 product_id UNIQUE로 재고 1건만 관리
-- =========================================================
CREATE TABLE p_stock (
                         id UUID PRIMARY KEY,
                         product_id UUID NOT NULL UNIQUE,
                         quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                         status stock_status NOT NULL DEFAULT 'AVAILABLE',
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         created_by UUID NOT NULL,
                         updated_at TIMESTAMP,
                         updated_by UUID,
                         deleted_at TIMESTAMP,
                         deleted_by UUID
);

CREATE INDEX idx_p_stock_status ON p_stock (status);
CREATE INDEX idx_p_stock_deleted_at ON p_stock (deleted_at);
CREATE INDEX idx_p_stock_product_id_status ON p_stock (product_id, status);

-- =========================================================
-- TABLE: p_stock_history
-- stock_id만 product-service 내부 물리 FK
-- order_id는 order-service 소속이므로 논리 FK
-- =========================================================
CREATE TABLE p_stock_history (
                                 id UUID PRIMARY KEY,
                                 stock_id UUID NOT NULL,
                                 type stock_history_type NOT NULL,
                                 quantity_change INTEGER NOT NULL,
                                 quantity_after INTEGER NOT NULL,
                                 order_id UUID, -- logical FK -> p_order.id
                                 created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                 created_by UUID NOT NULL,
                                 updated_at TIMESTAMP,
                                 updated_by UUID,
                                 deleted_at TIMESTAMP,
                                 deleted_by UUID,
                                 CONSTRAINT fk_p_stock_history_stock
                                     FOREIGN KEY (stock_id) REFERENCES p_stock(id)
);

CREATE INDEX idx_p_stock_history_stock_id ON p_stock_history (stock_id);
CREATE INDEX idx_p_stock_history_type ON p_stock_history (type);
CREATE INDEX idx_p_stock_history_order_id ON p_stock_history (order_id);
CREATE INDEX idx_p_stock_history_deleted_at ON p_stock_history (deleted_at);