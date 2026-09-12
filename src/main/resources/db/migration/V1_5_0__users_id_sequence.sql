-- Hibernate genera gli id della entity User tramite sequence (allocationSize 50).
-- La sequence non era mai stata creata dalle migration: esisteva solo sui database
-- generati a suo tempo da ddl-auto. Su un DB costruito dalle sole migration
-- (es. Testcontainers) la prima INSERT falliva con: relation "users_seq" does not exist.
CREATE SEQUENCE IF NOT EXISTS public.users_seq
    AS bigint
    INCREMENT BY 50
    START WITH 1
    OWNED BY public.users.id;

-- Su un database che ha gia' righe in users, riallinea la sequence sopra il max(id)
-- esistente per evitare collisioni di chiave primaria.
DO $$
DECLARE
    max_id bigint;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM public.users;
    IF max_id > 0 THEN
        PERFORM setval('public.users_seq', max_id + 1, false);
    END IF;
END $$;
