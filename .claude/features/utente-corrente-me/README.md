# Utente corrente (GET /me)

**Problema:** un endpoint che restituisce i dati dell'utente autenticato (nome, cognome, ...) senza che il client debba conoscere o passare il proprio id.
**Opzione scelta:** A — il principal resta l'id (`Long`), il controller carica l'utente dal database. Stack: Java 25, Spring Boot 3.5.14, Spring Security 6, PostgreSQL.

## Idea architetturale
Il filtro JWT non cambia: continua a mettere nel contesto di sicurezza solo l'id dell'utente, preso dal token (vedi `auth-jwt-login`, passo 6). Il nuovo endpoint riceve quell'id con `@AuthenticationPrincipal`, come già fa la creazione dei post, e chiede l'utente al service. I dati arrivano quindi sempre dal database e sono aggiornati. Solo questo endpoint paga la query, e il token continua a contenere solo id e scadenza.

## Workflow
1. Il client chiama `GET /users/me` con il token → 2. Spring Security blocca chi non ha un token valido → 3. Il controller riceve l'id dal principal → 4. Il service rifiuta un id mancante → 5. Il service carica l'utente, o risponde 404 se è stato cancellato → 6. Risponde 200 con i dati dell'utente

_Le righe indicate sono quelle del codice all'ultima modifica; se un numero non torna, cerca il metodo per nome._

### 1. Il client chiama `GET /users/me` con il token
**Cosa succede:** il client manda l'header `Authorization: Bearer <token>` e nessun id. Il filtro JWT valida il token e registra l'id dell'utente come identità della richiesta.
**Perché così:** il percorso è `/users/me` perché il metodo sta in `UserController`, che ha già il prefisso `users`. Non c'è conflitto con le altre rotte.
**Codice:** [`UserController.getCurrentUser`](../../../src/main/java/com/example/demo/user/controller/UserController.java#L30-L33)

### 2. Spring Security blocca chi non ha un token valido
**Cosa succede:** senza token, o con un token non valido o scaduto, la richiesta risponde 401 senza body e non arriva al controller.
**Perché così:** `/users/me` non è tra le rotte pubbliche, quindi vale la regola generale "tutto il resto richiede il token". Questo 401 nasce nella catena dei filtri, prima di Spring MVC, quindi il `GlobalExceptionHandler` non lo vede.
**Codice:** [`SecurityConfig.filterChain`](../../../src/main/java/com/example/demo/config/SecurityConfig.java#L31-L34)

### 3. Il controller riceve l'id dal principal
**Cosa succede:** Spring prende l'identità messa dal filtro e la passa al parametro `Long userId`. Se la richiesta è anonima, il principal è la stringa `"anonymousUser"` di Spring Security, che non è un `Long`, e il parametro riceve `null`. Il controller gira l'id al service senza controllarlo.
**Perché così:** qui, a differenza di `POST /posts`, non c'è `errorOnInvalidType`. Con quell'opzione una richiesta anonima darebbe una `ClassCastException` (500) invece di `null`, e il controllo del passo 4 non scatterebbe mai. Il prezzo è che un principal di tipo sbagliato qui diventa un 401 invece di un errore esplicito; resta però il controllo di `POST /posts`, che se ne accorgerebbe. Il controller non legge il contesto di sicurezza da solo, così il service si testa senza Spring Security.
**Codice:** [`UserController.getCurrentUser`](../../../src/main/java/com/example/demo/user/controller/UserController.java#L30-L33)

### 4. Il service rifiuta un id mancante
**Cosa succede:** se l'id è `null` (richiesta anonima, o nessuna autenticazione nel contesto), lancia `UnauthenticatedUserException` senza interrogare il database. Il `GlobalExceptionHandler` la trasforma in 401 con un `ProblemDetail` "Authentication required".
**Perché così:** è una seconda barriera. Con la configurazione attuale il passo 2 ferma prima ogni richiesta anonima, quindi questo ramo scatta solo se un giorno la rotta diventa pubblica o il metodo viene richiamato senza autenticazione. In quel caso la risposta resta un 401 chiaro invece di un errore generico. Il controllo sta nel service perché lì è testabile con un semplice unit test.
**Codice:** [`UserService.findCurrentUser`](../../../src/main/java/com/example/demo/user/service/UserService.java#L79-L84) · [`UnauthenticatedUserException`](../../../src/main/java/com/example/demo/user/exceptions/UnauthenticatedUserException.java) · [`GlobalExceptionHandler.handleUnauthenticatedUser`](../../../src/main/java/com/example/demo/utils/GlobalExceptionHandler.java#L125-L134) · [`UserErrors`](../../../src/main/java/com/example/demo/user/costants/UserErrors.java#L26-L27)

### 5. Il service carica l'utente, o risponde 404
**Cosa succede:** cerca l'utente per id e ne carica gli sport. Se l'utente non esiste più (cancellato dopo il login, con il token ancora valido), risponde 404 "User not found".
**Perché così:** riusa la stessa ricerca di `/users/userbyid`, quindi stesse regole e stessa risposta d'errore. È anche il modo in cui `/me` gestisce il limite noto "un utente cancellato resta autenticato fino alla scadenza".
**Codice:** [`UserService.findUserById`](../../../src/main/java/com/example/demo/user/service/UserService.java#L74-L77) · [`UserService.getUserById`](../../../src/main/java/com/example/demo/user/service/UserService.java#L86-L89) · [`GlobalExceptionHandler.handleUserNotFound`](../../../src/main/java/com/example/demo/utils/GlobalExceptionHandler.java#L72-L79)

### 6. Risponde 200 con i dati dell'utente
**Cosa succede:** restituisce id, nome, cognome, data di nascita e sport. Email e password non vengono restituite.
**Perché così:** si riusa `UserResponse` come in `/users/userbyid`. Se serve l'email, va creato un DTO dedicato, senza mai restituire l'entity, che contiene l'hash della password.
**Codice:** [`UserResponse`](../../../src/main/java/com/example/demo/user/dto/UserResponse.java)

## Classi e componenti
- [`UserController`](../../../src/main/java/com/example/demo/user/controller/UserController.java) — `GET /users/me` (`user.controller`)
- [`UserService`](../../../src/main/java/com/example/demo/user/service/UserService.java) — `findCurrentUser`: controllo sull'id e caricamento dell'utente (`user.service`)
- [`UnauthenticatedUserException`](../../../src/main/java/com/example/demo/user/exceptions/UnauthenticatedUserException.java) — nessun utente autenticato (`user.exceptions`)
- [`GlobalExceptionHandler`](../../../src/main/java/com/example/demo/utils/GlobalExceptionHandler.java), [`UserErrors`](../../../src/main/java/com/example/demo/user/costants/UserErrors.java) — risposta 401 "Authentication required" (`utils`, `user.costants`)
- Test: `UserServiceTest.FindCurrentUser`, `UserControllerIntegrationTests.GetCurrentUser`, `GlobalExceptionHandlerTests.handleUnauthenticatedUser_shouldReturnUnauthorized`, e `/users/me` aggiunto alle rotte protette in `JwtAuthenticationIntegrationTests`

## Avvio e deploy
- **Prima di avviare:** niente di nuovo. Servono le stesse cose del login (`JWT_SECRET`, PostgreSQL).
- **All'avvio parte da solo:** nessun bean, tabella o migrazione nuova.
- **In produzione:** una query sull'utente e una sugli sport a ogni chiamata di `/me`. Nessun dato scritto.
- **Verificato:** `./mvnw test` dà 192 test, tutti passano tranne i 3 già disabilitati (prima erano 182). Non verificato a mano con l'app avviata.
- **Assunzioni da adattare:** il percorso è `/users/me`, non `/me` alla radice. La risposta non contiene l'email.

## Se qualcosa non va
- 401 senza body → passo 2: manca il token, è scaduto o l'header non è esattamente `Bearer <token>`. Non viene dal `GlobalExceptionHandler`.
- 401 con `ProblemDetail` "Authentication required" → passo 4: la richiesta è arrivata al controller senza identità. Controlla se `/users/me` è finita tra le rotte pubbliche in `SecurityConfig`.
- 404 "User not found" con un token valido → passo 5: l'utente del token è stato cancellato.
- 401 "Authentication required" anche con un token valido → passo 3: il filtro mette nel principal un tipo diverso da `Long`, e qui arriva `null`. Lo conferma un 500 con `ClassCastException` su `POST /posts`.

## Note dello sviluppatore
> Spazio tuo: scrivi qui ciò che ti eri dimenticato di dire o che cambi in corsa.
> Il file viene riletto prima di scrivere codice e queste note hanno la precedenza.
