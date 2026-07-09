-- Auth service Flyway migration
CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    keycloak_session_id VARCHAR(255),
    refresh_token_hash  VARCHAR(64) NOT NULL,
    device_fingerprint  VARCHAR(128),
    ip_address          INET,
    user_agent          VARCHAR(500),
    mfa_verified        BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sessions_user ON auth.sessions (user_id) WHERE revoked_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sessions_expires ON auth.sessions (expires_at) WHERE revoked_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sessions_refresh_hash ON auth.sessions (refresh_token_hash);

CREATE TABLE IF NOT EXISTS auth.mfa_secrets (
    user_id             UUID PRIMARY KEY,
    totp_secret_enc     BYTEA NOT NULL,
    backup_codes_hash   TEXT[],
    enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at        TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS auth.login_audit (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_hash  VARCHAR(64) NOT NULL,
    ip_address  INET,
    success     BOOLEAN NOT NULL,
    reason      VARCHAR(100),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_login_audit_email ON auth.login_audit (email_hash, created_at DESC);
