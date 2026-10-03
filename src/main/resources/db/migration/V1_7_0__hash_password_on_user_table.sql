ALTER TABLE public.users
    ADD COLUMN hash_password varchar(255) NULL,
    ADD CONSTRAINT users_hash_password_not_blank CHECK (hash_password IS NULL OR length(btrim(hash_password)) > 0);
