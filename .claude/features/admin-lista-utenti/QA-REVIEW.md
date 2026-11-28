# QA Review — Endpoint admin-only per la lista utenti

**Branch / commit:** main, `cd9e989` + modifiche non committate (`UserController`, `UserService`)   **Data:** 2026-11-23
**Verdetto:** non pronta. Il codice è in lavorazione, non compila, e l'eccezione scelta produrrebbe un 500 invece di un 403.

## In breve
La feature vuole rendere `GET /users/allusers` accessibile solo agli `ADMIN`. L'approccio è imperativo: `UserService.findAll(long authorId)` carica il chiamante dal DB e lancia un'eccezione se non è admin. Il rischio principale è l'eccezione usata (`PermissionDeniedDataAccessException`), che appartiene al mondo database e non ha un handler, quindi il client riceverebbe 500. Il secondo è che tre suite di test esistenti usano `/users/allusers` come "endpoint protetto qualsiasi" con un utente normale e si romperanno.

## Cosa è stato verificato
- Build: `./mvnw -q compile` fallisce con tre errori (*verificato*):
  - `UserService.java:77`: `isAnAdmin` non esiste, il metodo si chiama `isAdmin`;
  - `UserService.java:78`: `Throwable` passato come valore, non è un'espressione valida;
  - `UserController.java:27`: chiama ancora `findAll()` senza argomenti.
- Suite esistente: non eseguibile finché il progetto non compila (*non verificato*).
- Chi usa `/users/allusers`, cercato con grep sui test (*verificato*):
  - `JwtAuthenticationIntegrationTests`: `PROTECTED_URL`, aspetta 200 con un token di un utente normale (riga 165);
  - `AuthLoginIntegrationTests`: riga 106, aspetta 200 dopo register + login;
  - `UserControllerIntegrationTests.GetAllUsers`: aspetta 200 con un chiamante `USER`.
- `UserServiceTest.FindAll`: chiama `findAll()` senza argomenti, quindi non compila più (*verificato*).

## Dove è stabile
- Il confronto `getRole() == Role.ADMIN` è corretto. `Role` è un enum, quindi `==` va bene, e la colonna è `NOT NULL` con `CHECK`, quindi non arrivano valori fuori dall'enum (*dedotto dal codice*).
- Un utente cancellato dopo l'emissione del token viene trattato come "non admin". Non lancia `NoSuchElementException`, perché l'`Optional` vuoto restituisce `false` (*dedotto dal codice*).

## Dove potrebbe rompersi
- **Eccezione sbagliata: 500 invece di 403.** Gravità alta, *dedotto dal codice*.
  - Dove: `UserService.findAll`, riga 78.
  - Meccanismo: `PermissionDeniedDataAccessException` è una `DataAccessException` di Spring, pensata per errori di permesso *del database* (per esempio un utente DB senza `GRANT`). `GlobalExceptionHandler` non la gestisce, e non è una `AccessDeniedException` di Spring Security, quindi nessuno la traduce in 403.
  - Scenario: un `USER` chiama `/users/allusers`.
- **Test esistenti che si romperanno.** Gravità media, *dedotto dal codice*.
  - Dove: i tre file elencati sopra.
  - Meccanismo: usano un chiamante `USER` e si aspettano 200. Va deciso se cambiano endpoint di riferimento o aspettativa (403).
- **Il ruolo non è nel contesto di sicurezza.** Gravità media, *dedotto dal codice*.
  - Dove: `JwtAuthenticationFilter`, riga 39.
  - Meccanismo: le authority sono `List.of()`, quindi Spring Security non sa chi è admin. Ogni controllo va rifatto a mano nei service, con una query in più ogni volta. La voce della checklist "role as claim/authority" è ancora aperta.
- **Nome del parametro fuorviante.** Gravità bassa, *dedotto dal codice*.
  - Dove: `UserController.getAllUsers` e `UserService.findAll`.
  - Meccanismo: si chiama `authorId`, copiato dai post. Qui è il chiamante, non un autore.

## Cose importanti da sapere
- `Optional` come parametro di metodo (`isAdmin(Optional<User>)`) è sconsigliato: `Optional` è pensato come tipo di ritorno.
- La Javadoc di `getUserByName` cita `{@link #getAllUsers()}` senza parametri. Dopo il cambio di firma il riferimento non è più valido e il build Javadoc lo segnalerà.
- `SecurityConfig` configura solo l'`authenticationEntryPoint` (401). Un `accessDeniedHandler` per il 403 non è configurato, quindi il formato del 403 non è un `ProblemDetail` come gli altri errori.

## Test che mancano
- `findAll` con un chiamante `ADMIN`: restituisce la lista.
- `findAll` con un chiamante `USER`: 403 con `ProblemDetail`, e la repository `findAll` non viene chiamata.
- `findAll` con un chiamante inesistente (token valido, utente cancellato): stessa risposta del caso non admin.
- Integrazione: `/users/allusers` senza token dà 401, con un `USER` dà 403, con un `ADMIN` dà 200.

## Non verificato
- Comportamento a runtime (status e formato delle risposte), perché il progetto non compila.

## Note dello sviluppatore
> Spazio tuo: risposte, contestazioni, cose che il QA non poteva sapere.
> Quando la recensione viene aggiornata, questa sezione resta com'è.
