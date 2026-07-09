CREATE SCHEMA IF NOT EXISTS fraud;

CREATE TABLE IF NOT EXISTS fraud.fraud_alerts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    transaction_id UUID,
    score SMALLINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    reasons JSONB NOT NULL DEFAULT '[]',
    rules_fired JSONB NOT NULL DEFAULT '[]',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolved_by UUID,
    resolution_note VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_fraud_alerts_user_id ON fraud.fraud_alerts (user_id);
CREATE INDEX idx_fraud_alerts_status ON fraud.fraud_alerts (status);
CREATE INDEX idx_fraud_alerts_created_at ON fraud.fraud_alerts (created_at DESC);
