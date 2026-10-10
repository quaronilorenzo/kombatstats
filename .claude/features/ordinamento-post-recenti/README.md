# Ordinamento post recenti

**Problema:** l'endpoint `GET /posts/recentposts` restituisce i post senza ordine garantito; devono arrivare dal più recente al più vecchio.
**Opzione scelta:** B — ordinamento passato come parametro `Sort` a `findAll(Sort)`. Stack: Java / Spring Boot, Spring Data JPA (spring-data-commons 3.5.x), Postgres.

## Idea architetturale
Il repository resta invariato: `PostRepository` eredita già da `JpaRepository` l'overload `findAll(Sort)`, quindi non serve dichiarare nessun metodo nuovo. L'ordinamento viene costruito nel service come oggetto `org.springframework.data.domain.Sort` sulla property `createdAt` in direzione discendente, e passato a `findAll`. Spring Data lo traduce in una clausola `ORDER BY created_at DESC` nell'SQL: l'ordinamento lo esegue il database, non la JVM — nessun sort in memoria sulla lista.

Il flusso resta quello attuale: `PostController.findAll` → `PostService.findAllPosts` → `PostRepository.findAll(Sort)` → `PostMapper.postsToPostResponses`. Cambia solo l'argomento della chiamata al repository.

## Clean code
- La definizione dell'ordine sta in **un posto solo**: una costante `private static final Sort` nel `PostService`, non una `Sort.by("createdAt")` ripetuta inline a ogni chiamata. La stringa della property non è controllata dal compilatore, quindi va scritta una volta.
- Nome della costante che esprima l'intento di dominio (es. `MOST_RECENT_FIRST`), non il meccanismo.
- Il controller non conosce l'ordinamento: resta una scelta del service. Se in futuro l'ordine deve arrivare dal client, allora il `Sort` diventa un parametro del metodo (Spring MVC sa iniettarlo dai query param) — ma non prima che serva davvero.
- Nessun ordinamento lato Java (`List.sort`, `Comparator`, stream `sorted`) su dati che il DB può già ordinare.

## Classi e componenti
- `PostService` (service layer, package `post.service`) — costruisce il `Sort` e lo passa al repository; unico file da toccare.
- `PostRepository` (repository, package `post.repository`) — invariato: `findAll(Sort)` è ereditato da `JpaRepository`.
- `Post` (entity, package `post.entity`) — fornisce la property `createdAt` (`Instant`, `@CreationTimestamp`, colonna `created_at`) su cui si ordina.
- `PostMapper` — invariato.

## Scelte significative
- **B invece di A (derived query `findAllByOrderByCreatedAtDesc`)** — scelto il `Sort` come parametro perché un solo metodo del repository regge N ordinamenti ed è la strada che si apre verso l'ordine scelto dal client; scartata A, che fissa l'ordinamento nel nome del metodo.
- **Niente limite sui risultati per ora** — l'endpoint `/recentposts` restituisce tutti i post ordinati. L'Opzione C (`Pageable` / `findTop...`, con `LIMIT` lato DB) resta il passo successivo quando il volume dei post cresce: richiede di passare da `List` a `Page` su service, controller e mapper.
- **Nome della property = `createdAt`** (campo Java), non `created_at` (colonna DB): Spring Data risolve la property sul metamodello JPA. L'errore iniziale (`At least one property must be given`) veniva da un `Sort.by(...)` chiamato senza nessun nome di property: la direzione da sola non basta mai.
- **Indice su `created_at`**: non creato. Da valutare (indice su `created_at`) solo se il numero di post cresce e l'`ORDER BY` diventa lento.

## Note dello sviluppatore
> Spazio tuo: scrivi qui ciò che ti eri dimenticato di dire o che cambi in corsa.
> Il file viene riletto prima di scrivere codice e queste note hanno la precedenza.
