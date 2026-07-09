CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE IF NOT EXISTS users.users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    email_hash      VARCHAR(64) NOT NULL,
    phone           VARCHAR(20),
    phone_hash      VARCHAR(64),
    password_hash   VARCHAR(255) NOT NULL DEFAULT 'KEYCLOAK',
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    country_code    CHAR(2) NOT NULL DEFAULT 'CD',
    kyc_level       SMALLINT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    tenant_id       UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    keycloak_id     UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_tenant ON users.users (email_hash, tenant_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_tenant ON users.users (phone_hash, tenant_id) WHERE phone_hash IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_keycloak_id ON users.users (keycloak_id);

CREATE TABLE IF NOT EXISTS users.profiles (
    user_id             UUID PRIMARY KEY REFERENCES users.users(id),
    date_of_birth       DATE,
    address_line1       VARCHAR(255),
    address_city        VARCHAR(100),
    address_country     CHAR(2),
    preferred_language  CHAR(2) DEFAULT 'fr',
    timezone            VARCHAR(50) DEFAULT 'Africa/Kinshasa',
    avatar_url          VARCHAR(500),
    metadata            JSONB DEFAULT '{}',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS users.data_export_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users.users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    download_url    VARCHAR(1000),
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_data_export_user ON users.data_export_requests (user_id, requested_at DESC);
