-- Sostituisce la vecchia @ElementCollection user_sport (user_id, sport) con una vera
-- relazione many-to-many fra users e sport, in grado di ospitare i dati di pratica.
--
-- years_practiced e is_competing stanno sulla tabella ponte, non su sport: dipendono
-- dalla coppia (utente, sport) e non dallo sport preso da solo. Se stessero su sport
-- servirebbe una riga "BJJ" per ogni praticante, e nessuno sport sarebbe piu'
-- condivisibile fra utenti.

-- 1. Mette da parte la tabella esistente: conserva i dati per il passo 4 e libera il nome.
ALTER TABLE public.user_sport RENAME TO user_sport_legacy;

-- 2. Anagrafica degli sport: cinque righe stabili, condivise da tutti gli utenti.
--    sport_type e' UNIQUE anche perche' il passo 4 ci fa JOIN sopra.
CREATE TABLE public.sport (
    id_sport   bigint GENERATED ALWAYS AS IDENTITY,
    sport_type varchar(255) NOT NULL,
    CONSTRAINT sport_pkey PRIMARY KEY (id_sport),
    CONSTRAINT sport_type_unique UNIQUE (sport_type),
    CONSTRAINT sport_type_check CHECK (sport_type IN ('BJJ', 'Boxing', 'MMA', 'Kickboxing', 'Wrestling'))
);

INSERT INTO public.sport (sport_type)
VALUES ('BJJ'), ('Boxing'), ('MMA'), ('Kickboxing'), ('Wrestling');

-- 3. Tabella ponte con i dati della pratica.
--    years_practiced e' nullable perche' il dato puo' non essere noto; numeric(4,1)
--    permette 0.1 per indicare circa un mese di pratica.
CREATE TABLE public.user_sport (
    id_user_sport   bigint GENERATED ALWAYS AS IDENTITY,
    id_user         bigint NOT NULL,
    id_sport        bigint NOT NULL,
    years_practiced numeric(4, 1) NULL,
    is_competing    boolean NOT NULL DEFAULT false,
    CONSTRAINT user_sport_pkey PRIMARY KEY (id_user_sport),
    CONSTRAINT user_sport_user_sport_unique UNIQUE (id_user, id_sport),
    CONSTRAINT user_sport_years_practiced_check CHECK (years_practiced >= 0),
    CONSTRAINT user_sport_id_user_fkey FOREIGN KEY (id_user) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT user_sport_id_sport_fkey FOREIGN KEY (id_sport) REFERENCES public.sport (id_sport) ON DELETE CASCADE
);

-- 4. Migra i dati esistenti.
--    DISTINCT e WHERE ... IS NOT NULL non sono difensivi per abitudine: la vecchia
--    user_sport non aveva ne' vincolo di unicita' ne' NOT NULL, quindi puo' contenere
--    coppie duplicate e righe con sport nullo che violerebbero i nuovi vincoli.
--    years_practiced resta NULL e is_competing false: sono dati che prima non
--    esistevano e non vanno inventati.
INSERT INTO public.user_sport (id_user, id_sport, is_competing)
SELECT DISTINCT l.user_id, s.id_sport, false
FROM public.user_sport_legacy l
JOIN public.sport s ON s.sport_type = l.sport
WHERE l.sport IS NOT NULL;

-- 5. Pulizia e indice sulla query principale ("gli sport di questo utente").
--    Su id_sport non serve: con cinque righe in sport il planner fa comunque una scan.
DROP TABLE public.user_sport_legacy;

CREATE INDEX idx_user_sport_id_user ON public.user_sport (id_user);
