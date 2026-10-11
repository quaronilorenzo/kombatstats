# Paginazione offset dei post

**Problema:** `GET /posts/recentposts` restituisce *tutti* i post in un'unica risposta. Serve consegnarli a pagine, con il totale degli elementi disponibile al client.
**Opzione scelta:** A — `Pageable` risolto da Spring MVC dai query param, `Page<PostResponse>` come ritorno. Stack: Java / Spring Boot 3.5.14, Spring Data JPA, Postgres, MapStruct 1.6.3, Flyway.

Prosegue la feature `ordinamento-post-recenti`: il `Sort` costante nel service viene sostituito dall'ordinamento che arriva nel `Pageable` (con default lato server).

## Idea architetturale
Il controller dichiara `Pageable` come parametro del metodo: `PageableHandlerMethodArgumentResolver` (attivo di default in Boot) lo costruisce da `?page=&size=&sort=`. Il `Pageable` scende invariato nel service, che lo **normalizza** (aggiunge il tie-breaker su `id`) e lo passa a `postRepository.findAll(Pageable)`. Il `Page<Post>` che torna viene convertito con `Page.map(postMapper::postToPostResponse)`, che preserva i metadati della pagina. Spring Data emette due query: la `SELECT ... LIMIT ? OFFSET ?` e una `SELECT count(*)` per il totale.

Flusso: `PostController.findAll(Pageable)` → `PostService.findAll(Pageable)` → `PostRepository.findAll(Pageable)` → `Page.map` → `Page<PostResponse>`.

Il JSON viene serializzato come `PagedModel` impostando la property `spring.data.web.pageable.serialization-mode=via_dto`: serializzare `PageImpl` direttamente non garantisce un contratto stabile tra versioni di Spring Data. Nessuna classe di configurazione: l'annotazione `@EnableSpringDataWebSupport` farebbe lo stesso lavoro ma disattiverebbe l'autoconfigurazione di Boot, e con essa tutte le altre property `spring.data.web.pageable.*`.

## Clean code
- **Il repository non si tocca:** `findAll(Pageable)` è ereditato. Nessun metodo nuovo da dichiarare.
- **Nessuna `List` sopravvive** nel percorso paginato: il tipo `Page` arriva fino al controller. Non convertire a `List` nel service perdendo i metadati, per poi ricostruirli.
- **La normalizzazione del `Pageable` sta nel service**, in un metodo privato con nome d'intento (es. `withStableSort`), non duplicata inline.
- **Il default dell'ordinamento sta sul controller** (`@PageableDefault`), perché è parte del contratto HTTP; il tie-breaker sta nel service, perché è un requisito di correttezza che vale per qualsiasi ordine.
- **Tutto ciò che riguarda il contratto di paginazione sta in `application.properties`** (`max-page-size`, `default-page-size`, `serialization-mode`), non in `if` sparsi nel codice ne in una classe `@Configuration`.
- Gli errori di paginazione passano da `GlobalExceptionHandler` come tutti gli altri, con `ProblemDetail` e costanti in `PostErrors`: nessun try/catch nel controller.

## Classi e componenti
- `PostController` (package `post.controller`) — dichiara `Pageable` + `@PageableDefault`, restituisce `Page<PostResponse>`.
- `PostService` (package `post.service`) — `findAll(Pageable)`: normalizza il `Pageable` in `withStableSort` (costante `TIE_BREAKER`) e mappa il `Page`.
- `GlobalExceptionHandler` (package `utils`) — nuovo `@ExceptionHandler` per `PropertyReferenceException` (`?sort=` su property inesistente) -> 400 invece di 500.
- `PostErrors` (package `post.costants`) — costanti `title`/`type` per quell'errore.
- `PostRepository`, `PostMapper`, `Post` — invariati.

## Scelte significative
- **A invece di B (`@RequestParam` + DTO proprio)** — scelto il `Pageable` iniettato per zero boilerplate e `sort` dal client gratis; scartato il DTO di paginazione fatto a mano.
- **`Page` invece di `Slice` (Opzione C)** — il totale serve al client; si accetta il costo della query di `count`.
- **Offset invece di keyset (Opzione D)** — volume dei post ancora piccolo e serve il salto a pagina arbitraria. Il keyset (`ScrollPosition` / `Window`, nativo da Spring Data 3.1) resta la migrazione futura se l'offset profondo diventa lento.
- **Tie-breaker `id DESC` obbligatorio** — senza, due post con lo stesso `createdAt` possono duplicarsi tra pagine o sparire.
- **`serialization-mode` via property, non via `@EnableSpringDataWebSupport`** — l'annotazione registra a mano il `PageableHandlerMethodArgumentResolver`, perciò `SpringDataWebAutoConfiguration` si disattiva e le property `spring.data.web.pageable.*` (`max-page-size` compresa) vengono ignorate in silenzio. La property le mantiene tutte attive.
- **Da decidere:** se accettare il `sort` dal client o imporre sempre l'ordine del server. Oggi: accettato, con default `createdAt,id DESC`.
- **`ResponseEntity<Page<PostResponse>>`** scelto per coerenza con gli altri endpoint, invece di `Page<PostResponse>` nudo. L'endpoint ha un solo esito possibile: 200, anche a pagina vuota — una pagina vuota non è un 404.
- **Indice su `(created_at DESC, id DESC)`** — non creato. Migrazione Flyway (prossima: `V1_12_0__...`) solo se l'`ORDER BY` diventa lento.

## Note dello sviluppatore
> Spazio tuo: scrivi qui ciò che ti eri dimenticato di dire o che cambi in corsa.
> Il file viene riletto prima di scrivere codice e queste note hanno la precedenza.
