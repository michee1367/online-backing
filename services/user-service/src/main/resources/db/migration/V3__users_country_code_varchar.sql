-- country_code : CHAR(2) -> VARCHAR(2) pour Hibernate validate
ALTER TABLE users.users
    ALTER COLUMN country_code TYPE VARCHAR(2) USING country_code::VARCHAR;
