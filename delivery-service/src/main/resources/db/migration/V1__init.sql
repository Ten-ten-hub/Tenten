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

CREATE TABLE p_delivery (
                            id UUID PRIMARY KEY,
                            order_id UUID NOT NULL UNIQUE,
                            delivery_status delivery_status NOT NULL,
                            origin_hub_id UUID NOT NULL,
                            destination_hub_id UUID NOT NULL,
                            receiver_company_id UUID NOT NULL,
                            delivery_address VARCHAR(255) NOT NULL,
                            delivery_address_detail VARCHAR(255),
                            recipient_name VARCHAR(100) NOT NULL,
                            recipient_slack_id VARCHAR(100) NOT NULL,
                            company_delivery_manager_id UUID,
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

CREATE TABLE p_delivery_route_log (
                                      id UUID PRIMARY KEY,
                                      delivery_id UUID NOT NULL,
                                      sequence_no INTEGER NOT NULL,
                                      departure_hub_id UUID NOT NULL,
                                      arrival_hub_id UUID NOT NULL,
                                      expected_distance_km DECIMAL(10,2) NOT NULL,
                                      expected_duration_minutes INTEGER NOT NULL,
                                      real_duration_minutes INTEGER,
                                      route_status delivery_route_status NOT NULL,
                                      delivery_manager_id UUID,
                                      departed_at TIMESTAMP,
                                      arrived_at TIMESTAMP,
                                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      created_by UUID NOT NULL,
                                      updated_at TIMESTAMP,
                                      updated_by UUID,
                                      deleted_at TIMESTAMP,
                                      deleted_by UUID,
                                      CONSTRAINT fk_p_delivery_route_log_delivery
                                          FOREIGN KEY (delivery_id) REFERENCES p_delivery(id)
);

ALTER TABLE p_delivery_route_log
    ADD CONSTRAINT uk_p_delivery_route_log_delivery_id_sequence_no
        UNIQUE (delivery_id, sequence_no);

CREATE INDEX idx_p_delivery_order_id ON p_delivery (order_id);
CREATE INDEX idx_p_delivery_status ON p_delivery (delivery_status);
CREATE INDEX idx_p_delivery_deleted_at ON p_delivery (deleted_at);

CREATE INDEX idx_p_delivery_route_log_delivery_id ON p_delivery_route_log (delivery_id);
CREATE INDEX idx_p_delivery_route_log_departure_hub_id ON p_delivery_route_log (departure_hub_id);
CREATE INDEX idx_p_delivery_route_log_arrival_hub_id ON p_delivery_route_log (arrival_hub_id);
CREATE INDEX idx_p_delivery_route_log_deleted_at ON p_delivery_route_log (deleted_at);

CREATE UNIQUE INDEX uk_p_delivery_order_id_active
    ON p_delivery(order_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_p_delivery_route_log_delivery_id_sequence_no_active
    ON p_delivery_route_log (delivery_id, sequence_no)
    WHERE deleted_at IS NULL;
