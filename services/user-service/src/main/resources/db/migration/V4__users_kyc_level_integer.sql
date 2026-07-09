-- kyc_level : SMALLINT -> INTEGER pour Hibernate validate (mapping int Java)
ALTER TABLE users.users
    ALTER COLUMN kyc_level TYPE INTEGER USING kyc_level::INTEGER;
