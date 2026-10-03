ALTER TABLE public.users
    DROP CONSTRAINT users_hash_password_not_blank,
    ALTER COLUMN hash_password SET NOT NULL,
    ADD CONSTRAINT users_hash_password_not_blank CHECK (length(btrim(hash_password)) > 0);
