ALTER TABLE wallets.wallets
    ADD COLUMN IF NOT EXISTS kyc_level SMALLINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_wallets_kyc_level ON wallets.wallets (kyc_level);
