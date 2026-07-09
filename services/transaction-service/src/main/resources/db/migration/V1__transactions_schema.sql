-- Flyway migration transaction-service
CREATE SCHEMA IF NOT EXISTS transactions;

CREATE TABLE IF NOT EXISTS transactions.transactions (
    id              UUID NOT NULL,
    idempotency_key UUID NOT NULL,
    type            VARCHAR(30) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source_wallet_id UUID,
    dest_wallet_id  UUID,
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    fee_amount      NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL,
    description     VARCHAR(255),
    initiated_by    UUID NOT NULL,
    fraud_score     SMALLINT,
    fraud_action    VARCHAR(20),
    signature       VARCHAR(128),
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    failed_reason   VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE IF NOT EXISTS transactions.transactions_default
    PARTITION OF transactions.transactions DEFAULT;

CREATE INDEX IF NOT EXISTS idx_txn_idempotency ON transactions.transactions (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_txn_source_wallet ON transactions.transactions (source_wallet_id, created_at DESC);

CREATE TABLE IF NOT EXISTS transactions.idempotency_keys (
    key             UUID PRIMARY KEY,
    response_status SMALLINT NOT NULL,
    response_body   JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '24 hours'
);
