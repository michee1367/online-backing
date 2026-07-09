CREATE SCHEMA IF NOT EXISTS banking;

CREATE TABLE IF NOT EXISTS banking.linked_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    bank_name VARCHAR(200) NOT NULL,
    bank_code VARCHAR(20),
    account_number_encrypted VARCHAR(500) NOT NULL,
    account_holder_name VARCHAR(200) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(25) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_linked_accounts_user ON banking.linked_accounts (user_id);

CREATE TABLE IF NOT EXISTS banking.bank_transfers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    wallet_id UUID NOT NULL,
    linked_account_id UUID REFERENCES banking.linked_accounts(id),
    transfer_type VARCHAR(15) NOT NULL,
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    swift_code VARCHAR(11),
    reference VARCHAR(50) NOT NULL UNIQUE,
    external_ref VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    failed_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_bank_transfers_user ON banking.bank_transfers (user_id);
CREATE INDEX idx_bank_transfers_ref ON banking.bank_transfers (reference);
