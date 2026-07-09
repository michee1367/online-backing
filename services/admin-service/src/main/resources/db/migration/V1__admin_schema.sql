CREATE SCHEMA IF NOT EXISTS admin;

CREATE TABLE IF NOT EXISTS admin.account_actions (
    id UUID PRIMARY KEY,
    target_user_id UUID NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    performed_by UUID NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_account_actions_target ON admin.account_actions (target_user_id);

CREATE TABLE IF NOT EXISTS admin.blacklist (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(20) NOT NULL,
    entity_value VARCHAR(255) NOT NULL,
    reason VARCHAR(500),
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_blacklist_entity ON admin.blacklist (entity_type, entity_value);

CREATE TABLE IF NOT EXISTS admin.system_config (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description VARCHAR(500),
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
