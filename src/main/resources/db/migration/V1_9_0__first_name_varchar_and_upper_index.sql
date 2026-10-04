DROP INDEX IF EXISTS idx_users_name;

ALTER TABLE public.users
    ALTER COLUMN first_name TYPE varchar(255) USING first_name::varchar(255);

CREATE INDEX idx_users_first_name_upper ON public.users (upper(first_name));
