# Private Nail Salon — backend

Spring Boot 4.1 / Java 21, PostgreSQL + Flyway.

## Run locally (dev profile, default)

```
docker compose up -d        # starts PostgreSQL on localhost:5433
./mvnw spring-boot:run
```

API listens on **http://localhost:8092**. Health check: `GET /actuator/health`.

## Run tests

```
./mvnw test
```

Uses the `test` profile (in-memory H2 in PostgreSQL-compatibility mode) — no Docker required.

## Profiles

| Profile | Database | Activated by |
|---|---|---|
| `dev` (default) | Local PostgreSQL via `docker-compose.yml` | default |
| `test` | In-memory H2 | `@ActiveProfiles("test")` in tests / CI |
| `prod` | PostgreSQL from `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD` env vars | `SPRING_PROFILES_ACTIVE=prod` |

## Error responses

All API errors return a consistent shape via `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-07-15T12:00:00Z",
  "status": 404,
  "code": "NOT_FOUND",
  "message": "...",
  "path": "/api/...",
  "fields": null
}
```

Throw `ApiException` (or its `notFound`/`badRequest`/`conflict` factories) from application code to
get a specific status/code; anything else is caught as a 500 without leaking internal details.

## Migrations

Flyway migrations live in `src/main/resources/db/migration`. None yet — see the README there.
