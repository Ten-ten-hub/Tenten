DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'analysis_type') THEN
            CREATE TYPE analysis_type AS ENUM ('DEADLINE', 'ROUTE');
        END IF;
    END
$$;

CREATE TABLE p_ai_analysis
(
    id            UUID PRIMARY KEY,
    order_id      UUID          NOT NULL,
    analysis_type analysis_type NOT NULL,
    input_data    JSONB         NOT NULL,
    output_data   JSONB         NOT NULL,
    ai_result     TEXT,

    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by    UUID          NOT NULL,
    updated_at    TIMESTAMP,
    updated_by    UUID,
    deleted_at    TIMESTAMP,
    deleted_by    UUID
);

CREATE INDEX idx_p_ai_analysis_order_id ON p_ai_analysis (order_id);
CREATE INDEX idx_p_ai_analysis_deleted_at ON p_ai_analysis (deleted_at);
