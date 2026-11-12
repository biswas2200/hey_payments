CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Append-only audit log for system events and financial operations
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor_id UUID,
    action VARCHAR(64) NOT NULL,
    entity VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64),
    data JSONB,
    ip VARCHAR(45),
    at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_audit_log_actor ON audit_log(actor_id, at DESC);
CREATE INDEX ix_audit_log_entity ON audit_log(entity, entity_id);

-- Enforce append-only guarantee on audit_log
CREATE OR REPLACE FUNCTION prevent_audit_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: UPDATE and DELETE operations are forbidden'
        USING ERRCODE = 'P0001';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_log_modification();

CREATE OR REPLACE FUNCTION prevent_audit_log_truncate()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: TRUNCATE is forbidden'
        USING ERRCODE = 'P0001';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_no_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT
    EXECUTE FUNCTION prevent_audit_log_truncate();

-- Idempotency keys fallback repository (Redis is primary, DB is persistent fallback)
CREATE TABLE idempotency_keys (
    key VARCHAR(128) PRIMARY KEY,
    user_id UUID,
    request_hash VARCHAR(64),
    response JSONB,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_idempotency_keys_user ON idempotency_keys(user_id, created_at DESC);

-- Spring Modulith Event Publication Registry schema
CREATE TABLE IF NOT EXISTS event_publication (
    id UUID PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_event_publication_serialized ON event_publication(event_type, completion_date);
