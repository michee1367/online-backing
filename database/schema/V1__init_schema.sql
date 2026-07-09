-- VillageSat Fintech — Schéma PostgreSQL initial
-- Version: 1.0.0
-- Requires: PostgreSQL 16+, extensions: pgcrypto, uuid-ossp

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- SCHEMA: users
-- ============================================================
CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    email_hash      VARCHAR(64) NOT NULL,  -- SHA-256 for lookup without decrypt
    phone           VARCHAR(20),
    phone_hash      VARCHAR(64),
    password_hash   VARCHAR(255) NOT NULL,  -- bcrypt, managed by Keycloak in prod
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    country_code    CHAR(2) NOT NULL DEFAULT 'CD',
    kyc_level       SMALLINT NOT NULL DEFAULT 0 CHECK (kyc_level BETWEEN 0 AND 3),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING_VERIFICATION'
                    CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','FROZEN','CLOSED')),
    tenant_id       UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    keycloak_id     UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,  -- Soft delete (RGPD)
    version         BIGINT NOT NULL DEFAULT 0  -- Optimistic locking
);

CREATE UNIQUE INDEX idx_users_email_tenant ON users.users (email_hash, tenant_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_users_phone_tenant ON users.users (phone_hash, tenant_id) WHERE phone_hash IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_users_status ON users.users (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_kyc_level ON users.users (kyc_level);

CREATE TABLE users.profiles (
    user_id         UUID PRIMARY KEY REFERENCES users.users(id),
    date_of_birth   DATE,
    address_line1   VARCHAR(255),
    address_city    VARCHAR(100),
    address_country CHAR(2),
    preferred_language CHAR(2) DEFAULT 'fr',
    timezone        VARCHAR(50) DEFAULT 'Africa/Kinshasa',
    avatar_url      VARCHAR(500),
    metadata        JSONB DEFAULT '{}',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SCHEMA: wallets
-- ============================================================
CREATE SCHEMA IF NOT EXISTS wallets;

CREATE TABLE wallets.wallets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,  -- FK cross-schema (eventual consistency)
    account_number  VARCHAR(20) NOT NULL UNIQUE,
    currency        VARCHAR(3) NOT NULL DEFAULT 'CDF',
    type            VARCHAR(20) NOT NULL DEFAULT 'PERSONAL'
                    CHECK (type IN ('PERSONAL','BUSINESS','ESCROW','SAVINGS')),
    label           VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    daily_limit     NUMERIC(19,4) NOT NULL DEFAULT 500.0000,
    monthly_limit   NUMERIC(19,4) NOT NULL DEFAULT 5000.0000,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_wallets_user_id ON wallets.wallets (user_id);
CREATE INDEX idx_wallets_currency ON wallets.wallets (currency);
CREATE INDEX idx_wallets_status ON wallets.wallets (status);

CREATE TABLE wallets.balances (
    wallet_id       UUID PRIMARY KEY REFERENCES wallets.wallets(id),
    balance         NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    available_balance NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (available_balance >= 0),
    pending_balance NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (pending_balance >= 0),
    last_transaction_at TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0  -- Pessimistic lock via SELECT FOR UPDATE
);

-- Double-entry ledger (append-only, partitioned)
CREATE TABLE wallets.ledger_entries (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
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

-- Create initial partitions (monthly)
CREATE TABLE wallets.ledger_entries_2026_05 PARTITION OF wallets.ledger_entries
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE wallets.ledger_entries_2026_06 PARTITION OF wallets.ledger_entries
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE INDEX idx_ledger_wallet_seq ON wallets.ledger_entries (wallet_id, entry_sequence DESC);
CREATE INDEX idx_ledger_transaction ON wallets.ledger_entries (transaction_id);

-- ============================================================
-- SCHEMA: transactions
-- ============================================================
CREATE SCHEMA IF NOT EXISTS transactions;

CREATE TABLE transactions.transactions (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    idempotency_key UUID NOT NULL,
    type            VARCHAR(30) NOT NULL
                    CHECK (type IN ('INTERNAL_TRANSFER','EXTERNAL_TRANSFER','DEPOSIT','WITHDRAWAL','PAYMENT','FEE')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','REVERSED','CANCELLED')),
    source_wallet_id UUID,
    dest_wallet_id  UUID,
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    fee_amount      NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL,
    description     VARCHAR(255),
    category        VARCHAR(30),
    external_ref    VARCHAR(100),
    fraud_score     SMALLINT,
    fraud_action    VARCHAR(20),
    initiated_by    UUID NOT NULL,  -- user_id
    signature       VARCHAR(128),  -- HMAC-SHA256 for high-value tx
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    failed_reason   VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE transactions.transactions_2026_05 PARTITION OF transactions.transactions
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE transactions.transactions_2026_06 PARTITION OF transactions.transactions
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE UNIQUE INDEX idx_txn_idempotency ON transactions.transactions (idempotency_key, created_at);
CREATE INDEX idx_txn_source_wallet ON transactions.transactions (source_wallet_id, created_at DESC);
CREATE INDEX idx_txn_dest_wallet ON transactions.transactions (dest_wallet_id, created_at DESC);
CREATE INDEX idx_txn_status ON transactions.transactions (status, created_at DESC);
CREATE INDEX idx_txn_initiated_by ON transactions.transactions (initiated_by, created_at DESC);

CREATE TABLE transactions.fees (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL,
    fee_type        VARCHAR(30) NOT NULL CHECK (fee_type IN ('TRANSFER','WITHDRAWAL','PAYMENT','FX','LATE')),
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    wallet_id       UUID NOT NULL,  -- Fee collected to platform wallet
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fees_transaction ON transactions.fees (transaction_id);

-- ============================================================
-- SCHEMA: payments
-- ============================================================
CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.merchants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    business_name   VARCHAR(200) NOT NULL,
    mcc_code        VARCHAR(4),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    settlement_wallet_id UUID,
    fee_rate        NUMERIC(5,4) NOT NULL DEFAULT 0.0150,  -- 1.5%
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments.payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL,
    merchant_id     UUID REFERENCES payments.merchants(id),
    payer_wallet_id UUID NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    order_reference VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments.qr_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID NOT NULL REFERENCES payments.merchants(id),
    payload_hash    VARCHAR(64) NOT NULL UNIQUE,
    amount          NUMERIC(19,4),
    currency        VARCHAR(3) NOT NULL DEFAULT 'CDF',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE','USED','EXPIRED','CANCELLED')),
    payment_id      UUID REFERENCES payments.payments(id),
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qr_merchant ON payments.qr_codes (merchant_id, status);
CREATE INDEX idx_qr_expires ON payments.qr_codes (expires_at) WHERE status = 'ACTIVE';

-- ============================================================
-- SCHEMA: compliance
-- ============================================================
CREATE SCHEMA IF NOT EXISTS compliance;

CREATE TABLE compliance.kyc_submissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    target_level    SMALLINT NOT NULL,
    document_type   VARCHAR(30) NOT NULL,
    document_number_enc BYTEA,  -- AES-256 encrypted
    document_front_key VARCHAR(500),
    document_back_key  VARCHAR(500),
    selfie_key      VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','IN_REVIEW','APPROVED','REJECTED','EXPIRED')),
    review_notes    TEXT,
    reviewed_by     UUID,
    provider_ref    VARCHAR(100),  -- Onfido/Jumio reference
    risk_score      NUMERIC(5,2),
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ
);

CREATE INDEX idx_kyc_user ON compliance.kyc_submissions (user_id, submitted_at DESC);
CREATE INDEX idx_kyc_status ON compliance.kyc_submissions (status) WHERE status IN ('PENDING','IN_REVIEW');

CREATE TABLE compliance.screenings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    screening_type  VARCHAR(30) NOT NULL CHECK (screening_type IN ('PEP','SANCTIONS','ADVERSE_MEDIA')),
    result          VARCHAR(20) NOT NULL CHECK (result IN ('CLEAR','MATCH','REVIEW')),
    provider        VARCHAR(50),
    details         JSONB,
    screened_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SCHEMA: fraud
-- ============================================================
CREATE SCHEMA IF NOT EXISTS fraud;

CREATE TABLE fraud.alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID,
    user_id         UUID NOT NULL,
    alert_type      VARCHAR(50) NOT NULL,
    severity        VARCHAR(10) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    score           SMALLINT NOT NULL,
    action_taken    VARCHAR(20) NOT NULL CHECK (action_taken IN ('ALLOW','REVIEW','BLOCK','STEP_UP')),
    rule_id         VARCHAR(50),
    details         JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN','INVESTIGATING','RESOLVED','FALSE_POSITIVE')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    resolved_by     UUID
);

CREATE INDEX idx_fraud_user ON fraud.alerts (user_id, created_at DESC);
CREATE INDEX idx_fraud_status ON fraud.alerts (status) WHERE status = 'OPEN';

-- ============================================================
-- SCHEMA: audit (append-only, immutable)
-- ============================================================
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_log (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    actor_id        UUID,
    actor_type      VARCHAR(20) NOT NULL CHECK (actor_type IN ('USER','ADMIN','SYSTEM','SERVICE')),
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(50) NOT NULL,
    resource_id     UUID,
    before_state    JSONB,
    after_state     JSONB,
    ip_address      INET,
    user_agent      VARCHAR(500),
    request_id      UUID,
    trace_id        VARCHAR(64),
    signature       VARCHAR(128) NOT NULL,  -- Ed25519 signature
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE audit.audit_log_2026_q2 PARTITION OF audit.audit_log
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');

CREATE INDEX idx_audit_actor ON audit.audit_log (actor_id, created_at DESC);
CREATE INDEX idx_audit_resource ON audit.audit_log (resource_type, resource_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit.audit_log (action, created_at DESC);

-- Prevent UPDATE/DELETE on audit log
CREATE OR REPLACE FUNCTION audit.prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit log is immutable. UPDATE and DELETE are forbidden.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_immutable
    BEFORE UPDATE OR DELETE ON audit.audit_log
    FOR EACH ROW EXECUTE FUNCTION audit.prevent_audit_modification();

-- ============================================================
-- SCHEMA: admin
-- ============================================================
CREATE SCHEMA IF NOT EXISTS admin;

CREATE TABLE admin.blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(20) NOT NULL CHECK (type IN ('PHONE','EMAIL','IP','DEVICE','ACCOUNT')),
    value_hash      VARCHAR(64) NOT NULL,
    reason          VARCHAR(100) NOT NULL,
    added_by        UUID NOT NULL,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_blacklist_type_value ON admin.blacklist (type, value_hash);

CREATE TABLE admin.account_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    action          VARCHAR(30) NOT NULL CHECK (action IN ('FREEZE','UNFREEZE','SUSPEND','CLOSE')),
    reason          VARCHAR(100) NOT NULL,
    notes           TEXT,
    performed_by    UUID NOT NULL,
    approved_by     UUID,  -- Four-eyes principle
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SCHEMA: auth
-- ============================================================
CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    device_fingerprint VARCHAR(128),
    ip_address      INET,
    user_agent      VARCHAR(500),
    mfa_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user ON auth.sessions (user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_sessions_expires ON auth.sessions (expires_at) WHERE revoked_at IS NULL;

CREATE TABLE auth.mfa_secrets (
    user_id         UUID PRIMARY KEY,
    totp_secret_enc BYTEA NOT NULL,  -- AES-256 encrypted TOTP secret
    backup_codes_hash TEXT[],  -- bcrypt hashed backup codes
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Outbox pattern table (per service, example for transactions)
-- ============================================================
CREATE TABLE transactions.outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON transactions.outbox_events (created_at)
    WHERE published_at IS NULL;

-- ============================================================
-- Idempotency keys (Redis primary, DB fallback)
-- ============================================================
CREATE TABLE transactions.idempotency_keys (
    key             UUID PRIMARY KEY,
    response_status SMALLINT NOT NULL,
    response_body   JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '24 hours'
);

CREATE INDEX idx_idempotency_expires ON transactions.idempotency_keys (expires_at);

-- ============================================================
-- Reconciliation
-- ============================================================
CREATE TABLE transactions.reconciliation_batches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider        VARCHAR(50) NOT NULL,
    batch_date      DATE NOT NULL,
    total_records   INTEGER NOT NULL,
    matched         INTEGER NOT NULL DEFAULT 0,
    unmatched       INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_recon_provider_date ON transactions.reconciliation_batches (provider, batch_date);
