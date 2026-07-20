ALTER TABLE users
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name  SET NOT NULL,
    ALTER COLUMN email      SET NOT NULL,
    ALTER COLUMN birth_date SET NOT NULL,
    ALTER COLUMN birth_date TYPE date USING birth_date::date,
    ADD CONSTRAINT users_email_unique    UNIQUE (email),
    ADD CONSTRAINT users_email_format    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    ADD CONSTRAINT users_birth_date_past CHECK (birth_date < CURRENT_DATE);
