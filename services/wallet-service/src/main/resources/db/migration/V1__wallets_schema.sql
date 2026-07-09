-- Flyway migration wallet-service
CREATE SCHEMA IF NOT EXISTS wallets;

CREATE TABLE IF NOT EXISTS wallets.wallets (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    account_number  VARCHAR(20) NOT NULL UNIQUE,
    currency        VARCHAR(3) NOT NULL DEFAULT 'CDF',
    type            VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    label           VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_limit     NUMERIC(19,4) NOT NULL DEFAULT 500.0000,
    monthly_limit   NUMERIC(19,4) NOT NULL DEFAULT 5000.0000,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets.wallets (user_id);

CREATE TABLE IF NOT EXISTS wallets.balances (
    wallet_id           UUID PRIMARY KEY REFERENCES wallets.wallets(id),
    balance             NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    available_balance   NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (available_balance >= 0),
    pending_balance     NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (pending_balance >= 0),
    last_transaction_at TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS wallets.ledger_entries (
    id              UUID NOT NULL,
    wallet_id       UUID NOT NULL REFERENCES wallets.wallets(id),
    transaction_id  UUID NOT NULL,
    entry_type      VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    balance_after   NUMERIC(19,4) NOT NULL,
    entry_sequence  BIGSERIAL,
    description     VARCHAR(255),
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE IF NOT EXISTS wallets.ledger_entries_default
    PARTITION OF wallets.ledger_entries DEFAULT;

CREATE INDEX IF NOT EXISTS idx_ledger_wallet_seq ON wallets.ledger_entries (wallet_id, entry_sequence DESC);
