/* =========================================================
   [ORDER-SERVICE]
   Tables:
   - p_order
   - p_order_item
   ========================================================= */

-- =========================================================
-- ENUM TYPE
-- =========================================================
CREATE TYPE order_status AS ENUM (
    'CREATED',
    'CONFIRMED',
    'READY_FOR_DELIVERY',
    'IN_DELIVERY',
    'CANCELLED',
    'COMPLETED'
);

-- =========================================================
-- TABLE: p_order
-- 서비스 간 참조는 논리 FK만 유지
-- =========================================================
CREATE TABLE p_order (
                         id UUID PRIMARY KEY,
                         ordered_by UUID NOT NULL, -- logical FK -> p_user.id
                         supplier_company_id UUID NOT NULL, -- logical FK -> p_company.id
                         receiver_company_id UUID NOT NULL, -- logical FK -> p_company.id
                         delivery_id UUID UNIQUE, -- logical FK -> p_delivery.id
                         deadline_at TIMESTAMP NOT NULL,
                         request_note VARCHAR(500),
                         total_price NUMERIC(15,2) NOT NULL,
                         order_status order_status NOT NULL DEFAULT 'CREATED',
                         cancelled_at TIMESTAMP,
                         cancelled_by UUID,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         created_by UUID NOT NULL,
                         updated_at TIMESTAMP,
                         updated_by UUID,
                         deleted_at TIMESTAMP,
                         deleted_by UUID
);

CREATE INDEX idx_p_order_ordered_by ON p_order (ordered_by);
CREATE INDEX idx_p_order_supplier_company_id ON p_order (supplier_company_id);
CREATE INDEX idx_p_order_receiver_company_id ON p_order (receiver_company_id);
CREATE INDEX idx_p_order_delivery_id ON p_order (delivery_id);
CREATE INDEX idx_p_order_deadline_at ON p_order (deadline_at);
CREATE INDEX idx_p_order_order_status ON p_order (order_status);
CREATE INDEX idx_p_order_deleted_at ON p_order (deleted_at);

-- =========================================================
-- TABLE: p_order_item
-- order_id만 order-service 내부 물리 FK
-- product_id는 product-service 소속이므로 논리 FK
-- =========================================================
CREATE TABLE p_order_item (
                              id UUID PRIMARY KEY,
                              order_id UUID NOT NULL,
                              product_id UUID NOT NULL, -- logical FK -> p_product.id
                              product_name_snapshot VARCHAR(100) NOT NULL,
                              unit_price_snapshot NUMERIC(15,2) NOT NULL,
                              quantity INTEGER NOT NULL CHECK (quantity > 0),
                              created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                              created_by UUID NOT NULL,
                              updated_at TIMESTAMP,
                              updated_by UUID,
                              deleted_at TIMESTAMP,
                              deleted_by UUID,
                              CONSTRAINT fk_p_order_item_order
                                  FOREIGN KEY (order_id) REFERENCES p_order(id)
);

CREATE INDEX idx_p_order_item_order_id ON p_order_item (order_id);
CREATE INDEX idx_p_order_item_product_id ON p_order_item (product_id);
CREATE INDEX idx_p_order_item_deleted_at ON p_order_item (deleted_at);

