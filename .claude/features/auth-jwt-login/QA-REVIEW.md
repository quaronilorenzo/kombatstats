# QA Review — Login con JWT

**Branch / commit:** `main`, `ba74984` → `9d3313e` (aggiornamento della review del 2026-11-01 su `5586b57`)   **Data:** 2026-11-07
**Verdetto:** pronta con riserve. Il filtro funziona e regge bene ai token malevoli. Restano però tre problemi da sistemare prima di considerarla chiusa: credenziali errate → 500, possibilità di pubblicare a nome di altri e test d'integrazione rimasti indietro.

## In breve
Rispetto alla review precedente sono stati aggiunti `JwtAuthenticationFilter`, le regole su `SecurityConfig` (stateless, `/auth/**` e `/error` pubbliche, il resto autenticato, 401 senza token) e la normalizzazione dell'email al login. Il rischio principale della volta scorsa ("il token non protegge nulla") è risolto. Ora il rischio maggiore è che l'identità del token non venga ancora usata: chi è autenticato può scrivere a nome di chiunque.

## Cosa è stato verificato
Ambiente: app locale sulla porta 8099, Postgres temporaneo in Docker con le migrazioni applicate a mano, segreto casuale. Tutto rimosso alla fine. Database di sviluppo non toccato.

- **Suite** (*verificato*): su HEAD 57 failure su 80 test; sul commit `5586b57`, 20 failure e 1 errore. **36 fallimenti nuovi**, tutti `401`: 29 test su `/posts` e 7 su `/users`. I test non inviano un token. L'errore di `shouldReturn400_whenSortPropertyIsUnknown` ora compare come 401, quindi è nascosto, non risolto. `KombatStatsApplicationTests` passa: il contesto si avvia.
- **Senza header o con token non valido → 401** (*verificato*): header assente; `Bearer ` vuoto o di soli spazi; stringa senza senso; firma alterata; `bearer` minuscolo; doppio spazio; `alg=none`; token scaduto; `sub` non numerico, assente o oltre il limite di `long`. Nessun 500.
- **Token valido → 200** su `/users/allusers` (*verificato*). Nessun `Set-Cookie` nella risposta, quindi niente sessione (*verificato*).
- **Errori dietro l'autenticazione** (*verificato*): con un token valido, un percorso inesistente → 404 e un JSON malformato → 400. Senza token, entrambi → 401.
- **Email con maiuscole al login → 200** (*verificato*): la riserva della volta scorsa è risolta.
- **Login con credenziali errate → 500** (*verificato*), sia con password sbagliata sia con email inesistente.
- **Pubblicare a nome di un altro** (*verificato*): token dell'utente A con `authorId` dell'utente B → 201, e il post risulta di B.
- **Timing** (*verificato*, 10 misure ciascuno): email esistente circa 89 ms, inesistente circa 10 ms.
- **Token con utente inesistente** (*verificato*, token firmato a mano con il segreto): `sub=987654` → 200.
- **Token senza `exp`** (*verificato*, firmato a mano): → 200, senza scadenza.
- **Preflight CORS**: `OPTIONS /posts` con `Origin` → 401 (*verificato*).

## Dove è stabile
- **Validazione del token nel filtro:** il `catch (JwtException | IllegalArgumentException)` copre anche `NumberFormatException`, che ne è sottoclasse. Tutti i casi malevoli provati finiscono in 401 e nessuno in 500.
- **Filtro registrato una sola volta:** è creato con `new` dentro `SecurityConfig` e non è un `@Component`, quindi Spring Boot non lo registra due volte.
- **Stateless:** nessuna sessione e nessun cookie.
- **Rotte pubbliche:** `/auth/register` e `/auth/login` funzionano senza token; `/error` aperto permette che 400 e 404 arrivino al client autenticato.

## Dove potrebbe rompersi

- **Si può scrivere a nome di un altro utente** — gravità: alta — verificato
  - Dove: `PostRequest.authorId` (riga 12), `PostService.addPost` (riga 40).
  - Meccanismo: il filtro mette l'id utente nel `SecurityContext`, ma nessuno lo legge. L'autore arriva ancora dal body.
  - Scenario: login come A, `POST /posts` con `authorId` di B → 201.

- **Credenziali errate → 500 invece di 401** — gravità: media — verificato
  - Dove: `LoginService.login` (riga 36); manca l'handler in `GlobalExceptionHandler`.
  - Meccanismo: `CredentialException` non è gestita e finisce in `/error` come errore interno. Un client non distingue una password sbagliata da un server rotto, e ogni tentativo produce un ERROR con stack trace nei log.

- **Test d'integrazione rimasti indietro** — gravità: media — verificato
  - Dove: `PostControllerIntegrationTests`, `UserControllerIntegrationTests`.
  - Meccanismo: le richieste MockMvc non hanno autenticazione. Oggi la suite non verifica nulla di `/posts` e `/users`, quindi una regressione lì passerebbe inosservata.

- **La protezione dal timing attack non funziona** — gravità: media — verificato
  - Dove: `AuthCostants.FAKE_PASSWORD` (riga 5).
  - Meccanismo: è testo in chiaro, non un hash BCrypt. `matches` scrive il WARN "does not look like BCrypt" (13 volte in questa sessione) e restituisce `false` senza calcolare. Invariato dalla review precedente.

- **Utente cancellato resta autenticato** — gravità: bassa — verificato con utente inesistente
  - Dove: `JwtAuthenticationFilter.doFilterInternal` (riga 39).
  - Meccanismo: il filtro si fida del `sub` e non controlla che l'utente esista. Oggi non c'è un endpoint di cancellazione; quando arriverà, il token resterà valido fino a 15 minuti. Il comportamento è coerente con "nessuna revoca" del README, ma conviene saperlo.

- **Token senza `exp` accettato per sempre** — gravità: bassa — verificato
  - Dove: `JwtService.extractUserId`.
  - Meccanismo: JJWT controlla `exp` solo se presente. Oggi non è sfruttabile, perché per firmare serve il segreto e `generateToken` imposta sempre la scadenza. Se però il segreto trapelasse, i token forgiati non scadrebbero mai.

- **`trim` dell'email inutile al login** — gravità: bassa — verificato
  - Dove: `LoginRequest` / `LoginService.login` (riga 31).
  - Meccanismo: `@Email` rifiuta `" qa@x.com "` con 400 prima che il service lo pulisca.

## Cose importanti da sapere
- **401 vuoto:** `HttpStatusEntryPoint` risponde senza body e senza header `WWW-Authenticate`. Il resto dell'API usa `ProblemDetail`, e un token scaduto non si distingue da un token assente.
- **CORS:** non è configurato. Un frontend su un'altra origine riceverà 401 sul preflight.
- **`logger` nel filtro:** il campo, non privato, nasconde il `logger` protetto ereditato da `OncePerRequestFilter`. Funziona, ma confonde.
- **Migrazioni:** `flyway-core` ha scope `test`, quindi l'app non applica le migrazioni all'avvio. È una scelta preesistente, ma va saputa per qualsiasi ambiente nuovo.
- **README vs codice:** il README prevede `InvalidCredentialsException` e la risposta 401. Il codice usa ancora `CredentialException` (JDK). L'import `BadRequestException` resta inutilizzato.
- **Algoritmo e `JWT_SECRET`:** invariati rispetto alla review precedente. L'algoritmo dipende dalla lunghezza della chiave, e i test che caricano il contesto richiedono `JWT_SECRET`.

## Test che mancano
- Filtro: nessun header, `Bearer ` vuoto, firma alterata, token scaduto, `sub` non numerico → 401; token valido → 200.
- Login: password errata ed email inesistente → stesso 401 e stesso corpo.
- `POST /posts`: l'autore deve essere quello del token, a prescindere dal body.
- `JwtService` unitario: genera, poi estrae lo stesso id.
- Aggiornare i test esistenti di `/posts` e `/users` per autenticarsi, per esempio con un helper che genera un token o con il supporto test di Spring Security.

## Non verificato
- Prestazioni e forza bruta sotto carico: non c'è un limite ai tentativi.
- Avvio con segreto non valido o di lunghezza diversa da 32 byte.
- Tempi su un'altra macchina: i millisecondi non si trasferiscono, il divario sì.

## Note dello sviluppatore
> Spazio tuo: risposte, contestazioni, cose che il QA non poteva sapere.
> Quando la recensione viene aggiornata, questa sezione resta com'è.
