-- NOTE:
-- 1) 이 파일은 참고용 통합 스키마 문서이다.
-- 2) 실제 MSA 적용 시에는 서비스별 V1__init.sql로 분리해서 사용한다.
-- 3) 타 서비스 엔티티 참조는 물리 FK 대신 논리 FK 기준으로 관리한다.

/* =========================================================
   TENTEN HUB - MASTER INIT REFERENCE SQL
   참고용 문서 / 실제 Flyway 실행용 아님
   서비스별로 분리해서 각 서비스의 V1__init.sql로 옮겨서 사용
   ========================================================= */

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


/* =========================================================
   [HUB-SERVICE]
   Tables:
   - p_hub
   - p_hub_route
   ========================================================= */

-- =========================================================
-- TABLE: p_hub
-- =========================================================
CREATE TABLE p_hub (
                       id UUID PRIMARY KEY,
                       hub_name VARCHAR(50) NOT NULL UNIQUE,
                       address VARCHAR(200) NOT NULL,
                       latitude DOUBLE PRECISION NOT NULL,
                       longitude DOUBLE PRECISION NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       created_by UUID NOT NULL,
                       updated_at TIMESTAMP,
                       updated_by UUID,
                       deleted_at TIMESTAMP,
                       deleted_by UUID
);

CREATE INDEX idx_p_hub_deleted_at ON p_hub (deleted_at);
CREATE INDEX idx_p_hub_address ON p_hub (address);

-- =========================================================
-- TABLE: p_hub_route
-- 같은 서비스 내부이므로 물리 FK 허용
-- =========================================================
CREATE TABLE p_hub_route (
                             id UUID PRIMARY KEY,
                             departure_hub_id UUID NOT NULL,
                             arrival_hub_id UUID NOT NULL,
                             duration INTEGER NOT NULL,
                             distance DOUBLE PRECISION NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             created_by UUID NOT NULL,
                             updated_at TIMESTAMP,
                             updated_by UUID,
                             deleted_at TIMESTAMP,
                             deleted_by UUID,
                             CONSTRAINT fk_p_hub_route_departure_hub
                                 FOREIGN KEY (departure_hub_id) REFERENCES p_hub(id),
                             CONSTRAINT fk_p_hub_route_arrival_hub
                                 FOREIGN KEY (arrival_hub_id) REFERENCES p_hub(id),
                             CONSTRAINT ck_p_hub_route_not_same_hub
                                 CHECK (departure_hub_id <> arrival_hub_id)
);

CREATE INDEX idx_p_hub_route_departure_hub_id ON p_hub_route (departure_hub_id);
CREATE INDEX idx_p_hub_route_arrival_hub_id ON p_hub_route (arrival_hub_id);
CREATE INDEX idx_p_hub_route_deleted_at ON p_hub_route (deleted_at);

CREATE UNIQUE INDEX uk_p_hub_route_active_departure_arrival
ON p_hub_route (departure_hub_id, arrival_hub_id)
WHERE deleted_at IS NULL;


/* =========================================================
   [COMPANY-SERVICE]
   Tables:
   - p_company
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
-- hub_id는 hub-service 소속이므로 논리 FK
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
                           deleted_by UUID,
                           CONSTRAINT uk_p_company_hub_id_name UNIQUE (hub_id, name)
);

CREATE INDEX idx_p_company_hub_id ON p_company (hub_id);
CREATE INDEX idx_p_company_company_type ON p_company (company_type);
CREATE INDEX idx_p_company_is_active ON p_company (is_active);
CREATE INDEX idx_p_company_deleted_at ON p_company (deleted_at);


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


/* =========================================================
   [DELIVERY-SERVICE]
   Tables:
   - p_delivery
   - p_delivery_route_log
   ========================================================= */

-- =========================================================
-- ENUM TYPE
-- =========================================================
CREATE TYPE delivery_status AS ENUM (
    'WAITING_AT_HUB',
    'MOVING_BETWEEN_HUBS',
    'ARRIVED_AT_DESTINATION_HUB',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED'
);

CREATE TYPE delivery_route_status AS ENUM (
    'WAITING_AT_HUB',
    'MOVING_BETWEEN_HUBS',
    'ARRIVED_AT_DESTINATION_HUB',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED'
);

-- =========================================================
-- TABLE: p_delivery
-- 서비스 간 참조는 논리 FK만 유지
-- =========================================================
CREATE TABLE p_delivery (
                            id UUID PRIMARY KEY,
                            order_id UUID NOT NULL UNIQUE, -- logical FK -> p_order.id
                            delivery_status delivery_status NOT NULL,
                            origin_hub_id UUID NOT NULL, -- logical FK -> p_hub.id
                            destination_hub_id UUID NOT NULL, -- logical FK -> p_hub.id
                            receiver_company_id UUID NOT NULL, -- logical FK -> p_company.id
                            delivery_address VARCHAR(255) NOT NULL,
                            delivery_address_detail VARCHAR(255),
                            recipient_name VARCHAR(100) NOT NULL,
                            recipient_slack_id VARCHAR(100) NOT NULL,
                            company_delivery_manager_id UUID, -- logical FK -> p_delivery_manager.id or p_user.id
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            final_dispatch_deadline_at TIMESTAMP,
                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            created_by UUID NOT NULL,
                            updated_at TIMESTAMP,
                            updated_by UUID,
                            deleted_at TIMESTAMP,
                            deleted_by UUID
);

CREATE INDEX idx_p_delivery_order_id ON p_delivery (order_id);
CREATE INDEX idx_p_delivery_origin_hub_id ON p_delivery (origin_hub_id);
CREATE INDEX idx_p_delivery_destination_hub_id ON p_delivery (destination_hub_id);
CREATE INDEX idx_p_delivery_receiver_company_id ON p_delivery (receiver_company_id);
CREATE INDEX idx_p_delivery_delivery_status ON p_delivery (delivery_status);
CREATE INDEX idx_p_delivery_deleted_at ON p_delivery (deleted_at);

-- =========================================================
-- TABLE: p_delivery_route_log
-- delivery_id만 delivery-service 내부 물리 FK
-- 나머지는 타 서비스 소속이므로 논리 FK
-- =========================================================
CREATE TABLE p_delivery_route_log (
                                      id UUID PRIMARY KEY,
                                      delivery_id UUID NOT NULL,
                                      sequence_no INTEGER NOT NULL,
                                      departure_hub_id UUID NOT NULL, -- logical FK -> p_hub.id
                                      arrival_hub_id UUID NOT NULL, -- logical FK -> p_hub.id
                                      expected_distance_km DECIMAL(10,2) NOT NULL,
                                      expected_duration_minutes INTEGER NOT NULL,
                                      route_status delivery_route_status NOT NULL,
                                      delivery_manager_id UUID, -- logical FK -> p_delivery_manager.id or p_user.id
                                      departed_at TIMESTAMP,
                                      arrived_at TIMESTAMP,
                                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      created_by UUID NOT NULL,
                                      updated_at TIMESTAMP,
                                      updated_by UUID,
                                      deleted_at TIMESTAMP,
                                      deleted_by UUID,
                                      CONSTRAINT fk_p_delivery_route_log_delivery
                                          FOREIGN KEY (delivery_id) REFERENCES p_delivery(id),
                                      CONSTRAINT uk_p_delivery_route_log_delivery_id_sequence_no
                                          UNIQUE (delivery_id, sequence_no)
);

CREATE INDEX idx_p_delivery_route_log_delivery_id ON p_delivery_route_log (delivery_id);
CREATE INDEX idx_p_delivery_route_log_departure_hub_id ON p_delivery_route_log (departure_hub_id);
CREATE INDEX idx_p_delivery_route_log_arrival_hub_id ON p_delivery_route_log (arrival_hub_id);
CREATE INDEX idx_p_delivery_route_log_route_status ON p_delivery_route_log (route_status);
CREATE INDEX idx_p_delivery_route_log_deleted_at ON p_delivery_route_log (deleted_at);


/* =========================================================
   [AI-SERVICE]
   Tables:
   - p_ai_analysis
   - p_notification
   ========================================================= */

-- =========================================================
-- ENUM TYPE
-- =========================================================
CREATE TYPE analysis_type AS ENUM (
    'DEADLINE',
    'ROUTE'
);

CREATE TYPE notification_msg_type AS ENUM (
    'ORDER_ALERT',
    'DAILY_REPORT'
);

CREATE TYPE notification_send_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAIL'
);

-- =========================================================
-- TABLE: p_ai_analysis
-- order_id는 order-service 소속이므로 논리 FK
-- =========================================================
CREATE TABLE p_ai_analysis (
                               id UUID PRIMARY KEY,
                               order_id UUID NOT NULL, -- logical FK -> p_order.id
                               analysis_type analysis_type NOT NULL,
                               input_data JSONB NOT NULL,
                               output_data JSONB NOT NULL,
                               ai_result TEXT,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               created_by UUID NOT NULL,
                               updated_at TIMESTAMP,
                               updated_by UUID,
                               deleted_at TIMESTAMP,
                               deleted_by UUID
);

CREATE INDEX idx_p_ai_analysis_order_id ON p_ai_analysis (order_id);
CREATE INDEX idx_p_ai_analysis_analysis_type ON p_ai_analysis (analysis_type);
CREATE INDEX idx_p_ai_analysis_deleted_at ON p_ai_analysis (deleted_at);

-- =========================================================
-- TABLE: p_notification
-- 서비스 간 참조는 논리 FK만 유지
-- =========================================================
CREATE TABLE p_notification (
                                id UUID PRIMARY KEY,
                                receiver_id UUID, -- logical FK -> p_user.id
                                receiver_slack_id VARCHAR(100) NOT NULL,
                                order_id UUID NOT NULL, -- logical FK -> p_order.id
                                msg_type notification_msg_type NOT NULL,
                                msg_content TEXT NOT NULL,
                                send_status notification_send_status NOT NULL DEFAULT 'PENDING',
                                ref_id UUID,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                created_by UUID NOT NULL,
                                updated_at TIMESTAMP,
                                updated_by UUID,
                                deleted_at TIMESTAMP,
                                deleted_by UUID
);

CREATE INDEX idx_p_notification_receiver_id ON p_notification (receiver_id);
CREATE INDEX idx_p_notification_order_id ON p_notification (order_id);
CREATE INDEX idx_p_notification_msg_type ON p_notification (msg_type);
CREATE INDEX idx_p_notification_send_status ON p_notification (send_status);
CREATE INDEX idx_p_notification_deleted_at ON p_notification (deleted_at);
