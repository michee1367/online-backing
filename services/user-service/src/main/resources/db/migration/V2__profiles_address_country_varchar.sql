-- Aligner les colonnes CHAR(2) du profil avec le mapping JPA (VARCHAR)
ALTER TABLE users.profiles
    ALTER COLUMN address_country TYPE VARCHAR(2) USING address_country::VARCHAR;

ALTER TABLE users.profiles
    ALTER COLUMN preferred_language TYPE VARCHAR(2) USING preferred_language::VARCHAR;
