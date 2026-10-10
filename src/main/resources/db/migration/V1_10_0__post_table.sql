CREATE TABLE public.post (
    id         bigint GENERATED ALWAYS AS IDENTITY,
    author_id  bigint       NOT NULL,
    title      varchar(255) NOT NULL,
    content    text         NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT post_pkey PRIMARY KEY (id),
    CONSTRAINT post_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT post_title_not_blank CHECK (length(btrim(title)) > 0)
);

CREATE INDEX idx_post_author_id ON public.post (author_id);
