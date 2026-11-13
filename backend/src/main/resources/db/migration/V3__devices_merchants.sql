-- V3__devices_merchants.sql: Devices, Merchants, and Server Keys
CREATE TABLE devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind VARCHAR(20) NOT NULL,
    public_key BYTEA NOT NULL,
    label VARCHAR(60),
    user_agent VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_counter BIGINT NOT NULL DEFAULT 0,
    frozen_at TIMESTAMPTZ,
    frozen_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_devices_kind CHECK (kind IN ('PAYER', 'MERCHANT')),
    CONSTRAINT ck_devices_status CHECK (status IN ('ACTIVE', 'FROZEN', 'REVOKED', 'CLOSED')),
    CONSTRAINT ck_devices_last_counter CHECK (last_counter >= 0)
);

-- Exactly one active device per user per kind
CREATE UNIQUE INDEX ux_device_active_user ON devices(user_id, kind) WHERE status = 'ACTIVE';
CREATE INDEX ix_devices_user ON devices(user_id);
CREATE INDEX ix_devices_status ON devices(status);

CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(60) NOT NULL,
    handle VARCHAR(60) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pending_cap BIGINT NOT NULL DEFAULT 1000000, -- 1000000 paise = ₹10,000
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_merchants_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_merchants_pending_cap CHECK (pending_cap > 0)
);

CREATE TABLE server_keys (
    kid VARCHAR(50) PRIMARY KEY,
    purpose VARCHAR(20) NOT NULL DEFAULT 'PROTOCOL',
    public_key BYTEA NOT NULL,
    private_key_enc BYTEA NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    not_before TIMESTAMPTZ NOT NULL,
    not_after TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_server_keys_purpose CHECK (purpose IN ('PROTOCOL', 'JWT')),
    CONSTRAINT ck_server_keys_status CHECK (status IN ('ACTIVE', 'RETIRING', 'REVOKED')),
    CONSTRAINT ck_server_keys_validity CHECK (not_after > not_before)
);

CREATE INDEX idx_server_keys_purpose_status ON server_keys (purpose, status, created_at DESC);
