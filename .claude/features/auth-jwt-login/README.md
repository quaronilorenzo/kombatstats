# Login con JWT

**Problema:** far accedere un utente con email e password, dargli un token e proteggere le rotte, che oggi sono tutte aperte.
**Opzione scelta:** B — JJWT con filtro scritto a mano; verifica credenziali V1, manuale in `LoginService`. Stack: Java 25, Spring Boot 3.5.14, Spring Security 6, PostgreSQL.

## Idea architetturale
Al login il server verifica le credenziali e restituisce un JWT: un token firmato che contiene l'id dell'utente e una scadenza. Il client lo rimanda a ogni richiesta e un filtro controlla firma e scadenza, senza sessioni lato server.

Il token è firmato, non cifrato: chiunque può leggerne il contenuto, ma non modificarlo senza invalidare la firma. Per questo contiene solo l'id e la scadenza.

## Workflow
1. Riceve e valida email e password → 2. Cerca l'utente → 3. Verifica la password → 4. Genera e restituisce il token → 5. Il client invia il token a ogni richiesta → 6. Il filtro lo valida e autentica la richiesta → 7. Spring Security applica le regole sulle rotte

## Decisioni prese
- **Validazione al login:** email con formato valido e password non vuota. La regex della password resta solo alla registrazione, così cambiare la policy non blocca chi ha una password vecchia.
- **Normalizzazione:** l'email passa da `trim` e minuscolo prima della ricerca, come alla registrazione.
- **Confronto password:** si usa la verifica di BCrypt, non un nuovo hash, perché il salt casuale rende ogni hash diverso.
- **Email inesistente:** si esegue comunque una verifica BCrypt su un hash fittizio, così i tempi di risposta sono simili a quelli di un'email esistente.
- **Credenziali errate:** una sola eccezione e una sola risposta 401, senza dire se è sbagliata l'email o la password.
- **Token:** algoritmo HS256 (firma con chiave segreta), soggetto uguale all'id utente, durata iniziale 15 minuti.
- **Segreto:** arriva da una variabile d'ambiente e non viene salvato nel repository.
- **Rotte pubbliche:** solo `/auth/**` e `/error`. Tutto il resto richiede il token: è una scelta iniziale, da allargare se alcune letture devono essere pubbliche.

## Limiti noti
- Un token non si può revocare prima della scadenza: non sono previsti logout lato server né refresh token.
- La registrazione risponde 409 se l'email è già in uso, quindi resta possibile scoprire quali email esistono.
- Non c'è ancora un limite ai tentativi di login.
- La creazione di un post prende l'autore dalla richiesta: un utente autenticato può quindi pubblicare a nome di un altro, finché l'autore non viene preso dal token.

## Classi e componenti
Da creare:
- `LoginRequest`, `LoginResponse` — dati in ingresso e in uscita del login (`user.dto`)
- `InvalidCredentialsException` — credenziali errate (`user.exceptions`)
- `LoginService` — già presente ma vuoto: verifica le credenziali e chiede il token (`user.service`)
- `JwtService` — genera e legge i token (`security`, package nuovo)
- `JwtAuthenticationFilter` — valida il token a ogni richiesta (`security`)

Da modificare:
- `AuthController` — aggiunge l'endpoint di login
- `GlobalExceptionHandler`, `UserErrors` — risposta 401 e relative costanti
- `SecurityConfig` — sessione stateless, regole sulle rotte, filtro, risposta 401 senza token
- `pom.xml`, `application.properties` (main e test) — dipendenze JJWT e configurazione del token

## Note dello sviluppatore
> Spazio tuo: scrivi qui ciò che ti eri dimenticato di dire o che cambi in corsa.
> Il file viene riletto prima di scrivere codice e queste note hanno la precedenza.
