CREATE SCHEMA IF NOT EXISTS compliance;

CREATE TABLE IF NOT EXISTS compliance.kyc_submissions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    target_level        SMALLINT NOT NULL CHECK (target_level BETWEEN 0 AND 3),
    document_type       VARCHAR(30) NOT NULL,
    document_number_enc BYTEA,
    document_front_key  VARCHAR(500),
    document_back_key   VARCHAR(500),
    selfie_key          VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','IN_REVIEW','APPROVED','REJECTED','EXPIRED')),
    review_notes        TEXT,
    reviewed_by         UUID,
    provider_ref        VARCHAR(100),
    risk_score          NUMERIC(5,2),
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at         TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_kyc_user ON compliance.kyc_submissions (user_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_kyc_status ON compliance.kyc_submissions (status)
    WHERE status IN ('PENDING','IN_REVIEW');

CREATE TABLE IF NOT EXISTS compliance.screenings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    kyc_submission_id UUID REFERENCES compliance.kyc_submissions(id),
    screening_type  VARCHAR(30) NOT NULL CHECK (screening_type IN ('PEP','SANCTIONS','ADVERSE_MEDIA')),
    result          VARCHAR(20) NOT NULL CHECK (result IN ('CLEAR','MATCH','REVIEW')),
    provider        VARCHAR(50),
    details         JSONB DEFAULT '{}',
    screened_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_screening_user ON compliance.screenings (user_id, screened_at DESC);

CREATE TABLE IF NOT EXISTS compliance.kyb_submissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    business_name   VARCHAR(200) NOT NULL,
    registration_number_enc BYTEA,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ
);
