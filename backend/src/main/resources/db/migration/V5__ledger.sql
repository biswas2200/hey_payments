-- V5: Ledger Accounts, Journal Entries, Journal Lines, Deferred Balance Trigger, and System Accounts Seed

CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    owner_id UUID,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ledger_accounts_code UNIQUE (code)
);

CREATE INDEX ix_ledger_accounts_owner ON ledger_accounts(owner_id);
CREATE INDEX ix_ledger_accounts_type ON ledger_accounts(type);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    kind VARCHAR(32) NOT NULL,
    reference VARCHAR(128) NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    memo VARCHAR(255),
    CONSTRAINT uq_journal_entries_reference UNIQUE (reference)
);

CREATE INDEX ix_journal_entries_posted ON journal_entries(posted_at DESC);

CREATE TABLE journal_lines (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES ledger_accounts(id) ON DELETE RESTRICT,
    amount BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_line_nonzero CHECK (amount <> 0)
);

CREATE INDEX ix_journal_lines_entry ON journal_lines(entry_id);
CREATE INDEX ix_journal_lines_account ON journal_lines(account_id);

-- Deferred balance trigger enforcing Sum(lines.amount) = 0 and >= 2 lines per journal entry at commit time
CREATE OR REPLACE FUNCTION check_journal_entry_balanced()
RETURNS TRIGGER AS $$
DECLARE
    v_count INT;
    v_sum BIGINT;
BEGIN
    SELECT COUNT(*), COALESCE(SUM(amount), 0)
    INTO v_count, v_sum
    FROM journal_lines
    WHERE entry_id = NEW.entry_id;

    IF v_count < 2 THEN
        RAISE EXCEPTION 'Journal entry % must have at least 2 lines, found %', NEW.entry_id, v_count;
    END IF;

    IF v_sum <> 0 THEN
        RAISE EXCEPTION 'Journal entry % is unbalanced: sum of amounts is %, expected 0', NEW.entry_id, v_sum;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_journal_balanced
AFTER INSERT OR UPDATE ON journal_lines
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION check_journal_entry_balanced();

-- Seed System Accounts (Idempotent)
INSERT INTO ledger_accounts (id, code, type, owner_id, currency, balance, version, created_at, updated_at)
VALUES 
    ('01930000-0000-7000-8000-000000000001', 'SYS:BANK_SIM_CLEARING', 'BANK_SIM_CLEARING', NULL, 'INR', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('01930000-0000-7000-8000-000000000002', 'SYS:PLATFORM_FEES', 'PLATFORM_FEES', NULL, 'INR', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('01930000-0000-7000-8000-000000000003', 'SYS:RISK_RESERVE', 'RISK_RESERVE', NULL, 'INR', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
