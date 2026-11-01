# QA Review — Login con JWT

**Branch / commit:** `main`, `ba74984` → `5586b57`   **Data:** 2026-11-01
**Verdetto:** non pronta — il login corretto funziona, ma il token non protegge nulla e la protezione dal timing attack non è attiva.

## In breve
La feature aggiunge `POST /auth/login`, che verifica le credenziali e restituisce un JWT. Il percorso corretto regge e `JwtService` si è comportato bene sui token alterati. Manca però la metà che usa il token: filtro e regole di `SecurityConfig` non esistono, quindi ogni rotta resta aperta. Il rischio più grande è che "Working login" sembri voler dire "autenticazione attiva", e non è così.

## Cosa è stato verificato
Ambiente: app locale con Postgres temporaneo e segreto casuale, poi rimossi. Database di sviluppo non toccato.

- Suite su HEAD: 86 test, 20 failure, 1 errore. Sul commit base lo stesso risultato: **nessun fallimento nuovo** (*verificato*).
- Login corretto → 200; token HS256 con solo `sub`, `iat`, `exp`; durata 900 secondi (*verificato*).
- Validazione: password vuota, campi assenti, email non valida e JSON malformato → 400; GET → 405 (*verificato*).
- `JwtService.extractUserId` con programma esterno: token alterato, altra chiave, scaduto, malformato, `alg=none` → tutti rifiutati con `JwtException` (*verificato*).
- Segreto corto, vuoto o non Base64 → eccezione nel costruttore di `JwtService`, che impedisce di crearlo (*verificato sul costruttore, non sull'avvio completo*).
- Password di 5000 caratteri al login: circa 90 ms, nessun rallentamento (*verificato*).

## Dove è stabile
- **Firma e scadenza del token:** i casi malevoli provati vengono rifiutati.
- **Contenuto del token e risposte:** nessun dato personale nel token, nessun hash nelle risposte di registrazione e login.
- **Segreto:** nessun valore reale nei file di configurazione, nei commit della feature o in `http/`.
- **Registrazione:** `UserService` cambia solo per spazi vuoti; il resto del diff non tocca rotte esistenti.

## Dove potrebbe rompersi

- **Il token non viene mai controllato** — gravità: alta — verificato
  - Dove: `SecurityConfig.filterChain` (riga 20); filtro assente.
  - Meccanismo: `anyRequest().permitAll()` è invariato e nessuno chiama `extractUserId`.
  - Scenario: `GET /users/allusers` senza token o con token inventato → 200, con nomi e date di nascita.

- **Si può scrivere a nome di un altro utente** — gravità: alta — verificato
  - Dove: `PostRequest.authorId` (riga 12), `PostService.addPost` (riga 40).
  - Meccanismo: l'autore arriva dal client. Il solo filtro non basta: un utente autenticato potrebbe ancora indicare l'id di un altro.
  - Scenario: `POST /posts` senza token con l'id di un altro utente → 201, post attribuito a quell'utente.

- **La protezione dal timing attack non funziona** — gravità: media — verificato
  - Dove: `AuthCostants.FAKE_PASSWORD` (riga 5), `LoginService.login` (righe 33-35).
  - Meccanismo: il valore è testo in chiaro, non un hash BCrypt. L'encoder scrive il WARN "Encoded password does not look like BCrypt" e restituisce `false` senza calcolare.
  - Scenario: email esistente circa 91 ms, inesistente circa 11 ms (due serie da 10 misure). Sistemare l'handler non cambia questi tempi.
  - Limite: `/auth/register` rivela comunque quali email esistono, con il 409.

- **Chi si registra con maiuscole non riesce a entrare** — gravità: media — verificato
  - Dove: `LoginService.login` (riga 31).
  - Meccanismo: la registrazione salva l'email in minuscolo, il login la cerca così com'è.
  - Scenario: registrazione con `QA.User@Example.com`, login con la stessa stringa → fallisce; in minuscolo → 200.

- **Il filtro rischia errori 500 con alcuni token** — gravità: media — verificato su `JwtService`, filtro non ancora scritto
  - Dove: `JwtService.extractUserId` (righe 38-45).
  - Meccanismo: non tutti gli errori sono `JwtException`. Una stringa vuota o blank lancia `IllegalArgumentException`; un token firmato senza soggetto numerico lancia `NumberFormatException`.
  - Scenario: header `Authorization: Bearer ` senza token. Il `catch` della sola `JwtException`, indicato nella guida allo step 8, non basta.

- **Crash futuro alla riga 38** — gravità: media — dedotto dal codice
  - Dove: `LoginService.login` (riga 38).
  - Meccanismo: `user.get()` è raggiunto solo se `matches` è vero. Oggi non accade con un'email inesistente, ma se `FAKE_PASSWORD` diventa l'hash di una password nota, chi la invia con un'email inesistente ottiene `NoSuchElementException`.

- **Registrazione con password multibyte → 500** — gravità: bassa — verificato
  - Dove: `UserRequest` (riga 27), `UserService.register` (riga 45).
  - Meccanismo: la regex conta 72 caratteri, BCrypt accetta 72 byte.
  - Scenario: 64 caratteri, 124 byte (con `é`) → `IllegalArgumentException`, 500. Il problema è precedente al login: quel codice non è cambiato.

- **La password oltre 72 byte viene troncata al login** — gravità: bassa — verificato
  - Dove: `LoginRequest.password`.
  - Meccanismo: la verifica considera solo i primi 72 byte.
  - Scenario: utente con password di 72 caratteri; la stessa password con `EXTRA` in coda → 200.

## Cose importanti da sapere
- **Algoritmo variabile:** `signWith` lo sceglie dalla lunghezza della chiave: 32 byte HS256, 48 HS384, 64 HS512 (*verificato*). Il README dichiara HS256 e lo rispetta solo con un segreto da 32 byte.
- **`JWT_SECRET` obbligatoria anche nei test:** senza la variabile, `KombatStatsApplicationTests` fallisce con `Could not resolve placeholder` (*verificato*). In CI o su un altro PC non partirà nessun test che carica il contesto Spring.
- **Log:** ogni login fallito produce oggi un ERROR con stack trace di circa 100 righe. Le password non compaiono nei log (*verificato sui valori usati*). Il WARN di BCrypt resta finché l'hash fittizio non è valido.
- **`CredentialException`:** oltre all'handler, va sostituita o gestita anche la `throws` nel controller. L'import `BadRequestException` in `LoginService` non è usato.
- **Altri test già rotti:** i 20 failure sono causati da `POST /users` → 404, rotta spostata su `/auth/register`. L'errore rimanente, `shouldReturn400_whenSortPropertyIsUnknown`, è un problema reale ma separato: un sort sconosciuto non produce 400.

## Test che mancano
Oggi nessun test copre `/auth/login` o `JwtService`.
- Login corretto, password errata, email inesistente: stesso 401 e stesso corpo.
- Email con maiuscole o spazi.
- Hash fittizio: `matches` eseguito davvero quando l'utente non esiste.
- `JwtService`: token valido, alterato, scaduto, vuoto, soggetto non numerico.
- Rotte con e senza token, una volta aggiunto il filtro.
- `POST /posts`: l'autore deve coincidere con l'utente del token.

## Non verificato
- Filtro e rotte protette: non esistono ancora.
- Risposta 401 finale: l'handler non c'è, oggi le credenziali errate danno 500.
- Prestazioni e attacchi a forza bruta sotto carico: nessuna prova di carico, e nessun limite ai tentativi nel codice.
- Avvio dell'app con segreto non valido: provato solo il costruttore di `JwtService`.
- Tempi su un ambiente diverso da questo PC: i valori in millisecondi non sono trasferibili, il divario sì.

## Note dello sviluppatore
> Spazio tuo: risposte, contestazioni, cose che il QA non poteva sapere.
> Quando la recensione viene aggiornata, questa sezione resta com'è.
