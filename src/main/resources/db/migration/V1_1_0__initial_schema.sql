CREATE TABLE public.users (
	id int8 NOT NULL,
	birth_date timestamp(6) NULL,
	email varchar(255) NULL,
	first_name varchar(255) NULL,
	last_name varchar(255) NULL,
	CONSTRAINT users_pkey PRIMARY KEY (id)
);

CREATE TABLE public.user_sport (
	user_id int8 NOT NULL,
	sport varchar(255) NULL,
	CONSTRAINT user_sport_sport_check CHECK (((sport)::text = ANY ((ARRAY['BJJ'::character varying, 'Boxing'::character varying, 'MMA'::character varying, 'Kickboxing'::character varying, 'Wrestling'::character varying])::text[]))),
	CONSTRAINT fkfrwwwd4t6aevl389ovvlse1uj FOREIGN KEY (user_id) REFERENCES public.users(id)
);
