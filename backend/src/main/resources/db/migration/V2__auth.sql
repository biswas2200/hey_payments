-- V2__auth.sql: Users and Refresh Tokens
CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pin_hash VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_role CHECK (role IN ('PAYER', 'MERCHANT', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE UNIQUE INDEX ux_users_phone ON users(phone);
CREATE INDEX ix_users_email ON users(email);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX ix_refresh_tokens_user_family ON refresh_tokens(user_id, family_id);
