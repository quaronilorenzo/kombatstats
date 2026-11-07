# Login con JWT

**Problema:** far accedere un utente con email e password, dargli un token e proteggere le rotte, che oggi sono tutte aperte.
**Opzione scelta:** B — JJWT con filtro scritto a mano; verifica credenziali V1, manuale in `LoginService`. Stack: Java 25, Spring Boot 3.5.14, Spring Security 6, PostgreSQL.

## Idea architetturale
Al login il server verifica le credenziali e restituisce un JWT: un token firmato che contiene l'id dell'utente e una scadenza. Il client lo rimanda a ogni richiesta e un filtro controlla firma e scadenza, senza sessioni lato server.

Il token è firmato, non cifrato: chiunque può leggerne il contenuto, ma non modificarlo senza invalidare la firma. Per questo contiene solo l'id e la scadenza.

## Workflow
1. Riceve e valida email e password → 2. Cerca l'utente → 3. Verifica la password → 4. Genera e restituisce il token → 5. Il client invia il token a ogni richiesta → 6. Il filtro lo valida e autentica la richiesta → 7. Spring Security applica le regole sulle rotte → 8. I controller che ne hanno bisogno ricevono l'id dell'utente autenticato

_Le righe indicate sono quelle del codice all'ultima modifica; se un numero non torna, cerca il metodo per nome._

### 1. Riceve e valida email e password
**Cosa succede:** il login accetta un'email con formato valido e una password non vuota; altrimenti risponde 400.
**Perché così:** la regex della password vale solo alla registrazione, così chi ha una password vecchia può ancora entrare.
**Codice:** [`AuthController.login`](../../../src/main/java/com/example/demo/user/controller/AuthController.java#L41-L44) · [`LoginRequest`](../../../src/main/java/com/example/demo/user/dto/LoginRequest.java#L6-L12)

### 2. Cerca l'utente
**Cosa succede:** l'email viene ridotta in minuscolo e ripulita dagli spazi, poi cercata nel database.
**Perché così:** è la stessa normalizzazione della registrazione. Gli spazi però vengono rifiutati già al passo 1, quindi oggi il `trim` non ha effetto.
**Codice:** [`LoginService.login`](../../../src/main/java/com/example/demo/user/service/LoginService.java#L30-L31)

### 3. Verifica la password
**Cosa succede:** BCrypt confronta la password ricevuta con l'hash salvato. Se l'utente non esiste, il confronto avviene con un valore fittizio.
**Perché così:** con un hash fittizio i tempi di risposta non dovrebbero rivelare quali email esistono. Oggi il valore fittizio non è un hash BCrypt, quindi questa protezione non è attiva. Le credenziali errate generano un'eccezione senza handler, cioè un 500, non il 401 previsto.
**Codice:** [`LoginService.login`](../../../src/main/java/com/example/demo/user/service/LoginService.java#L32-L37) · [`AuthCostants`](../../../src/main/java/com/example/demo/user/costants/AuthCostants.java)

### 4. Genera e restituisce il token
**Cosa succede:** crea un JWT firmato con l'id dell'utente come soggetto e una scadenza di 15 minuti, e lo restituisce come `accessToken`.
**Perché così:** il token contiene solo id e scadenza, perché chiunque può leggerlo.
**Codice:** [`JwtService.generateToken`](../../../src/main/java/com/example/demo/security/JwtService.java#L30-L36) · [configurazione](../../../src/main/resources/application.properties#L6-L7)

### 5. Il client invia il token a ogni richiesta
**Cosa succede:** il client lo mette nell'header `Authorization`, nella forma `Bearer <token>`.
**Perché così:** è il formato standard, ed è quello che il filtro si aspetta: esattamente "Bearer" seguito da un solo spazio.

### 6. Il filtro valida il token e autentica la richiesta
**Cosa succede:** se l'header c'è, il filtro toglie il prefisso e verifica firma e scadenza. Se il token è valido, registra la richiesta come autenticata, con l'id dell'utente (un `Long`) come identità. Se non lo è, scrive una riga di debug e lascia la richiesta anonima.
**Perché così:** il filtro non risponde mai da solo. Decide il passo 7, così le rotte pubbliche funzionano anche con un token sbagliato.
**Codice:** [`JwtAuthenticationFilter.doFilterInternal`](../../../src/main/java/com/example/demo/security/JwtAuthenticationFilter.java#L26-L48) · [`JwtService.extractUserId`](../../../src/main/java/com/example/demo/security/JwtService.java#L39-L46)

### 7. Spring Security applica le regole sulle rotte
**Cosa succede:** `/auth/**` e `/error` sono aperte. Tutto il resto risponde 401, senza body, alle richieste anonime. Non vengono create sessioni.
**Perché così:** con le sessioni disattivate, ogni richiesta deve portare il proprio token.
**Codice:** [`SecurityConfig.filterChain`](../../../src/main/java/com/example/demo/config/SecurityConfig.java#L22-L36)

### 8. I controller ricevono l'id dell'utente autenticato
**Cosa succede:** alla creazione di un post, il controller riceve l'id messo dal filtro e lo passa al service, che lo usa come autore. Un eventuale `authorId` nel body viene ignorato.
**Perché così:** l'identità ha una sola fonte, il token, e nessuno può pubblicare a nome di altri. Il tipo del parametro deve essere `Long` come quello del filtro: con un tipo diverso Spring passerebbe `null`, e l'opzione `errorOnInvalidType` trasforma quel `null` in un errore esplicito. Il service non legge il contesto di sicurezza, quindi si testa senza Spring Security.
**Codice:** [`PostController.addPost`](../../../src/main/java/com/example/demo/post/controller/PostController.java#L44-L48) · [`PostService.addPost`](../../../src/main/java/com/example/demo/post/service/PostService.java#L37-L49) · [`PostRequest`](../../../src/main/java/com/example/demo/post/dto/PostRequest.java#L8-L16)

## Decisioni prese
- **Validazione al login:** email con formato valido e password non vuota. La regex della password resta solo alla registrazione, così cambiare la policy non blocca chi ha una password vecchia.
- **Normalizzazione:** l'email passa da `trim` e minuscolo prima della ricerca, come alla registrazione.
- **Confronto password:** si usa la verifica di BCrypt, non un nuovo hash, perché il salt casuale rende ogni hash diverso.
- **Email inesistente:** si esegue comunque una verifica BCrypt su un hash fittizio, così i tempi di risposta sono simili a quelli di un'email esistente.
- **Credenziali errate:** una sola eccezione e una sola risposta 401, senza dire se è sbagliata l'email o la password.
- **Token:** algoritmo HS256 (firma con chiave segreta), soggetto uguale all'id utente, durata iniziale 15 minuti.
- **Segreto:** arriva da una variabile d'ambiente e non viene salvato nel repository.
- **Autore di un post (opzione A):** è sempre l'utente del token, ricevuto con `@AuthenticationPrincipal`. La richiesta non contiene più `authorId` (dettagli al passo 8).
- **Rotte pubbliche:** solo `/auth/**` e `/error`. Tutto il resto richiede il token: è una scelta iniziale, da allargare se alcune letture devono essere pubbliche.

## Limiti noti
- Un token non si può revocare prima della scadenza: non sono previsti logout lato server né refresh token.
- La registrazione risponde 409 se l'email è già in uso, quindi resta possibile scoprire quali email esistono.
- Non c'è ancora un limite ai tentativi di login.
- Il filtro non controlla che l'utente del token esista ancora: un utente cancellato resta autenticato fino alla scadenza.

## Classi e componenti
- [`AuthController`](../../../src/main/java/com/example/demo/user/controller/AuthController.java) — registrazione e login (`user.controller`)
- [`LoginRequest`](../../../src/main/java/com/example/demo/user/dto/LoginRequest.java), [`LoginResponse`](../../../src/main/java/com/example/demo/user/dto/LoginResponse.java) — dati in ingresso e in uscita del login (`user.dto`)
- [`LoginService`](../../../src/main/java/com/example/demo/user/service/LoginService.java) — verifica le credenziali e chiede il token (`user.service`)
- [`JwtService`](../../../src/main/java/com/example/demo/security/JwtService.java) — genera e legge i token (`security`)
- [`JwtAuthenticationFilter`](../../../src/main/java/com/example/demo/security/JwtAuthenticationFilter.java) — valida il token a ogni richiesta (`security`)
- [`SecurityConfig`](../../../src/main/java/com/example/demo/config/SecurityConfig.java) — stateless, regole sulle rotte, filtro, 401 senza token (`config`)
- [`PostController`](../../../src/main/java/com/example/demo/post/controller/PostController.java), [`PostService`](../../../src/main/java/com/example/demo/post/service/PostService.java), [`PostRequest`](../../../src/main/java/com/example/demo/post/dto/PostRequest.java) — creazione di un post con l'autore preso dal token (`post`)
- **Ancora da fare:** `InvalidCredentialsException` e il suo handler in `GlobalExceptionHandler` (oggi si usa `CredentialException` del JDK, senza handler); un hash BCrypt vero in `AuthCostants`.

## Avvio e deploy
- **Prima di avviare:** la variabile d'ambiente `JWT_SECRET`, una chiave Base64 di almeno 32 byte. Con 32 byte la firma è HS256; con più byte JJWT passa a HS384 o HS512. I test usano un segreto fisso in `src/test/resources`.
- **All'avvio parte da solo:** il filtro e le regole di sicurezza. Nessuna tabella nuova, nessuna migrazione.
- **In produzione:** non c'è stato lato server, quindi più istanze funzionano purché condividano lo stesso `JWT_SECRET`. Cambiare il segreto invalida tutti i token emessi. `POST /posts` non accetta più `authorId`: i client che lo mandano ancora non ricevono errori, ma il campo viene ignorato.
- **Verificato:** `./mvnw clean test` dopo la modifica dei post dà 179 test: 172 passano, 7 rossi, 3 disabilitati. I 7 rossi sono i punti ancora da fare al passo 3, l'email con spazi al passo 2 e il sort sconosciuto, che non riguarda questa feature. Il test "mai attribuito a un altro utente" ora passa. Non verificato a mano con l'app avviata.

## Se qualcosa non va
- 401 con un token che sembra valido → passo 6: controlla che l'header sia esattamente `Bearer ` + token e che il token non sia scaduto (15 minuti). Il filtro scrive "Jwt is not valid" solo a livello debug.
- 500 sul login con credenziali errate → passo 3: manca l'handler per `CredentialException`.
- 500 o errore di tipo su `POST /posts` con un token valido → passo 8: il parametro con `@AuthenticationPrincipal` deve avere lo stesso tipo del principal messo dal filtro (`Long`).
- 404 "user not found" su `POST /posts` → passo 8: l'utente del token è stato cancellato dopo il login.
- L'app non parte con "Could not resolve placeholder" → manca `JWT_SECRET`.

## Note dello sviluppatore
> Spazio tuo: scrivi qui ciò che ti eri dimenticato di dire o che cambi in corsa.
> Il file viene riletto prima di scrivere codice e queste note hanno la precedenza.
