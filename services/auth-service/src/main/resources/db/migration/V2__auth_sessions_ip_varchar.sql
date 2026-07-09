ALTER TABLE auth.sessions
    ALTER COLUMN ip_address TYPE varchar(45) USING ip_address::text;
