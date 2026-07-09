CREATE SCHEMA IF NOT EXISTS mobile_money;

CREATE TABLE IF NOT EXISTS mobile_money.transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    wallet_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'CDF',
    transaction_type VARCHAR(15) NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'INITIATED',
    external_ref VARCHAR(100) UNIQUE,
    provider_ref VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    failed_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_mm_user_id ON mobile_money.transactions (user_id);
CREATE INDEX idx_mm_external_ref ON mobile_money.transactions (external_ref);

CREATE TABLE IF NOT EXISTS mobile_money.provider_config (
    provider VARCHAR(20) PRIMARY KEY,
    api_url VARCHAR(500),
    api_key_encrypted VARCHAR(500),
    merchant_id VARCHAR(100),
    callback_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO mobile_money.provider_config (provider, api_url, active)
VALUES ('MPESA', 'https://sandbox.safaricom.co.ke', true),
       ('ORANGE_MONEY', 'https://api.orange.com/orange-money-webpay', true),
       ('MTN_MOMO', 'https://sandbox.momodeveloper.mtn.com', true),
       ('AIRTEL_MONEY', 'https://openapi.airtel.africa', true)
ON CONFLICT DO NOTHING;
