# KombatStats

A Spring Boot REST API for managing combat-sports athletes: personal data (name, email, date of birth) and the disciplines they practice (BJJ, Boxing, MMA, Kickboxing, Wrestling). The project is under active development.

## Tech Stack

| Layer      | Technology                                                       |
|------------|-------------------------------------------------------------------|
| Language   | Java 25                                                            |
| Framework  | Spring Boot 3.5.14 (Web, Data JPA, Security, Validation)           |
| Database   | PostgreSQL                                                         |
| Migrations | Flyway (run via the Maven plugin — see note below)                 |
| Mapping    | MapStruct                                                          |
| Testing    | JUnit 5, Mockito, Spring Security Test                             |
| Build tool | Maven (wrapper included, no local install required)                |

## Prerequisites

Before pulling this project, make sure you have:

- **JDK 25** installed and selected (`java -version`)
- **Git**
- **PostgreSQL** reachable at `localhost:5433` (not the default `5432`), either installed locally or run via Docker (see below). You do **not** need Maven installed — this repo ships the Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd KombatStats
```

### 2. Start PostgreSQL

The app expects a database reachable at `jdbc:postgresql://localhost:5433/postgres` with user `kombat` / password `kombat` (see `src/main/resources/application.properties` and `flyway.conf`). These are local development defaults committed to the repo — fine for running locally, but don't reuse them for anything beyond your machine.

The quickest way to get a matching instance with Docker (the container's `kombat` user is created as superuser, which is needed for the `citext` extension used by the migrations):

```bash
docker run --name kombatstats-db \
  -e POSTGRES_USER=kombat \
  -e POSTGRES_PASSWORD=kombat \
  -e POSTGRES_DB=postgres \
  -p 5433:5432 \
  -d postgres
```

If you'd rather use an existing local PostgreSQL install, create a superuser role `kombat`/`kombat` and make sure the server listens on port `5433` (or update `application.properties` and `flyway.conf` to match your setup).

### 3. Run the database migrations

Flyway is wired in as a **build plugin only** (there is no `flyway-core` runtime dependency), so migrations are **not** applied automatically on application startup, and Hibernate's `ddl-auto` isn't configured either — nothing will create the schema for you. Run this once (and again any time a new migration is added) before starting the app:

```bash
./mvnw flyway:migrate      # macOS/Linux/Git Bash
mvnw.cmd flyway:migrate    # Windows
```

This applies the SQL files under `src/main/resources/db/migration`, creating the `users` and `user_sport` tables.

### 4. Run the application

```bash
./mvnw spring-boot:run      # macOS/Linux/Git Bash
mvnw.cmd spring-boot:run    # Windows
```

The API starts on `http://localhost:8080`.

## API Overview

| Method | Endpoint             | Description                          |
|--------|-----------------------|---------------------------------------|
| POST   | `/users`              | Create a new user                     |
| GET    | `/users/allusers`     | List all users                        |
| GET    | `/users/userbyid`     | Get a user by `id` (query param)      |
| GET    | `/users/userbyname`   | Get a user by `name` (first name, case-insensitive) |

Example — create a user:

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
        "firstName": "Claudia",
        "lastName": "Rategni",
        "email": "claudia.rategni@gmail.com",
        "birthDate": "2007-04-13",
        "sport": ["BJJ", "MMA"]
      }'
```

Valid `sport` values: `BJJ`, `Boxing`, `MMA`, `Kickboxing`, `Wrestling`.

## Running Tests

```bash
./mvnw test      # macOS/Linux/Git Bash
mvnw.cmd test     # Windows
```

`UserServiceTest` and `GlobalExceptionHandlerTests` are pure unit tests (Mockito) and need nothing extra. `KombatStatsApplicationTests` boots the full Spring context with `@SpringBootTest`, so **PostgreSQL must be running and migrated (steps 2–3 above) for the full test suite to pass.**

## Project Structure

```
src/main/java/com/example/demo/
├── KombatStatsApplication.java     # entry point
├── config/SecurityConfig.java      # security filter chain
├── user/
│   ├── controller/                 # REST endpoints
│   ├── service/                    # business logic
│   ├── repository/                 # Spring Data JPA repository
│   ├── entity/                     # User entity, Sport enum
│   ├── dto/                        # request/response records + MapStruct mapper
│   ├── exceptions/                 # domain exceptions
│   └── costants/                   # error messages/URIs
└── utils/GlobalExceptionHandler.java  # RFC 9457 problem-detail error responses
src/main/resources/db/migration/    # Flyway SQL migrations
```

## Notes

- **Security is fully open** for now: `SecurityConfig` permits all requests and disables CSRF. This is a local-development state, not a production-ready configuration.
- The app uses the default `postgres` database/`public` schema rather than a dedicated database — update `application.properties` and `flyway.conf` together if you change this.
