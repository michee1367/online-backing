CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.audit_log (
    id UUID NOT NULL,
    service_name VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    user_id UUID,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE IF NOT EXISTS audit.audit_log_default PARTITION OF audit.audit_log DEFAULT;

CREATE INDEX idx_audit_entity ON audit.audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit.audit_log (user_id);
CREATE INDEX idx_audit_event_type ON audit.audit_log (event_type);
