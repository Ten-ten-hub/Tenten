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
                       name VARCHAR(50) NOT NULL UNIQUE,
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

-- =========================================================
-- TABLE: p_hub_route
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
                             CONSTRAINT fk_p_hub_route_departure
                                 FOREIGN KEY (departure_hub_id) REFERENCES p_hub(id),
                             CONSTRAINT fk_p_hub_route_arrival
                                 FOREIGN KEY (arrival_hub_id) REFERENCES p_hub(id)
);

-- 논리적 삭제된 데이터는 중복 체크에서 제외하는 부분 유니크 인덱스
CREATE UNIQUE INDEX uk_p_hub_route_active
    ON p_hub_route (departure_hub_id, arrival_hub_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_p_hub_route_departure ON p_hub_route (departure_hub_id);
CREATE INDEX idx_p_hub_route_arrival ON p_hub_route (arrival_hub_id);
CREATE INDEX idx_p_hub_route_deleted_at ON p_hub_route (deleted_at);
