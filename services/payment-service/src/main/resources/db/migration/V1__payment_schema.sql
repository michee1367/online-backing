CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE IF NOT EXISTS payments.merchants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    business_type VARCHAR(50),
    merchant_code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    callback_url VARCHAR(500),
    commission_rate NUMERIC(5,4) NOT NULL DEFAULT 0.0200,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS payments.payments (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES payments.merchants(id),
    customer_id UUID,
    wallet_id UUID,
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    fee NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'CDF',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) NOT NULL DEFAULT 'QR_CODE',
    reference VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    merchant_order_id VARCHAR(100),
    qr_code_data TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    failed_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_payments_merchant_id ON payments.payments (merchant_id);
CREATE INDEX idx_payments_reference ON payments.payments (reference);
CREATE INDEX idx_payments_customer_id ON payments.payments (customer_id);
