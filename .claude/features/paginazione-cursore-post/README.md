# Paginazione a cursore dei post

**Problema:** `GET /posts/recentpostsoffset` deve restituire `CursorPage<PostResponse>` (`items`, `nextCursor`, `hasNext`) con valori corretti. Oggi restituisce l'entità `Post`, `nextCursor` è il cursore ricevuto e `hasNext` è sempre `true`.
**Opzione scelta:** B — `Slice<Post>` dal repository. Stack: Java / Spring Boot 3.5.14, Spring Data JPA, Postgres, MapStruct 1.6.3.

Feature distinta da `paginazione-offset-post`: quella resta su `Page` + offset per `/recentposts`.

## Idea architetturale
Il client manda `lastId` (assente alla prima pagina) e `size`. Il service costruisce un `Pageable` sempre con `page = 0`: la posizione la dà `lastId`, non il numero di pagina. `PostRepository.findNextPage` restituisce `Slice<Post>`: Spring Data legge `size + 1` righe e calcola `hasNext()` senza query di count. Il service mappa lo `Slice` a `PostResponse` con `Slice.map`, prende come `nextCursor` l'`id` dell'ultimo elemento (o `null`) e costruisce il `CursorPage`.

Flusso: `PostController.findAllOffset(lastId, size)` → `PostService.findNextPosts` → `PostRepository.findNextPage(lastId, Pageable)` → `Slice.map` → `CursorPage<PostResponse>`.

## Clean code
- **Nessuna entità nel JSON:** `CursorPage<PostResponse>`, mai `CursorPage<Post>` (`author` LAZY → errore proxy Hibernate o leak di `hashPassword`).
- **`page` sempre 0:** mai combinare offset e cursore nella stessa query.
- **Il cursore si calcola dopo la query**, dall'ultimo elemento del contenuto, non dall'input.
- **`nextCursor = null` quando `hasNext` è false** (contratto per il client: stop).
- `CursorPage` resta generico in `utils.dto`: nessuna logica dentro il record.
- Il limite di `size` (min/max) è parte del contratto HTTP: validato sul controller, non con `if` sparsi nel service.

## Classi e componenti
- `CursorPage<T, C>` (`utils.dto`) — record `items`, `nextCursor` (tipo `C`, nullable), `hasNext`.
- `PostCursor` (`post.dto`) — record `createdAt`, `id`: il cursore dei post.
- `PostRepository.findNextPage` (`post.repository`) — ritorno da `List<Post>` a `Slice<Post>`, `@Query` invariata.
- `PostService.findNextPosts` (`post.service`) — ritorna `CursorPage<PostResponse>`.
- `PostController.findAllOffset` (`post.controller`) — ritorna `CursorPage<PostResponse>`.
- `PostMapper` — invariato (`postToPostResponse` usato da `Slice.map`).

## Scelte significative
- **B invece di A (`size + 1` a mano)** — il "c'è altro?" lo calcola Spring Data; scartato il taglio manuale.
- **B invece di C (`Window` / `ScrollPosition`)** — cursore solo su `id`, numero semplice; `Window` rimandato a quando si ordinerà per `(createdAt, id)`.
- **Ordine per `(createdAt DESC, id DESC)`**, cursore composto — sostituisce l'ordine solo per `id`. `createdAt` da solo non è univoco: serve l'`id` come tie-breaker nella `WHERE` (`createdAt < c OR (createdAt = c AND id < i)`). Usa l'indice `V1_12_0__post_created_at_id_index`. Resta B con la condizione scritta a mano; l'alternativa C (`Window`) la genererebbe da sola.
- **Due query separate, `findFirstPage` e `findNextPage`**, invece di una con `:param IS NULL`: con un parametro null Postgres non ne ricava il tipo e fallisce. Se il cursore è incompleto il service usa `findFirstPage`.
- **Il client rimanda entrambi i valori** (`lastCreatedAt`, `lastId`): `nextCursor` non può più essere un `Long`.
- **Da decidere:** rinominare l'endpoint (`recentpostsoffset` è fuorviante: è a cursore).

## Note dello sviluppatore
> Spazio tuo: scrivi qui ciò che ti eri dimenticato di dire o che cambi in corsa.
> Il file viene riletto prima di scrivere codice e queste note hanno la precedenza.
