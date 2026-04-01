CREATE TABLE p_delivery_manager (
                                    id UUID PRIMARY KEY,
                                    hub_id UUID,
                                    slack_id VARCHAR(100) NOT NULL,
                                    delivery_manager_type VARCHAR(50) NOT NULL,
                                    delivery_sequence INTEGER NOT NULL,
                                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    created_by UUID NOT NULL,
                                    updated_at TIMESTAMP,
                                    updated_by UUID,
                                    deleted_at TIMESTAMP,
                                    deleted_by UUID
);

CREATE INDEX idx_p_delivery_manager_hub_id ON p_delivery_manager (hub_id);
CREATE INDEX idx_p_delivery_manager_type ON p_delivery_manager (delivery_manager_type);
CREATE INDEX idx_p_delivery_manager_deleted_at ON p_delivery_manager (deleted_at);

CREATE UNIQUE INDEX uk_p_delivery_manager_sequence_active
    ON p_delivery_manager (delivery_manager_type, hub_id, delivery_sequence)
    WHERE deleted_at IS NULL;
