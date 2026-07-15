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

## Owner auth (CRM)

Session-based (Secure/HttpOnly `SESSION` cookie) with SPA CSRF: call `GET /api/v1/admin/auth/csrf`
once to receive the readable `XSRF-TOKEN` cookie, then mirror it in the `X-XSRF-TOKEN` header on
every mutation. Login is rate-limited (10 attempts / 15 min per IP+email).

- `POST /api/v1/admin/auth/login` · `POST .../logout` · `GET .../me`
- `POST .../password/request-reset` (always 202) · `POST .../password/reset`

Dev profile seeds an owner: `owner@nailsalon.local` / `owner-dev-password` (see `DevDataSeeder`),
plus a demo business profile and small catalog.

## Catalog admin API

`/api/v1/admin/categories` and `/api/v1/admin/services` (+ nested `/addons`) — CRUD, archive via
`PATCH .../status`, reorder via `PUT .../order`. Optimistic locking: send the entity's `version`
on every edit; a stale version returns 409. Full contract: `src/main/resources/openapi/openapi.yaml`.

## Communications (foundation only — no real SMS)

Provider-neutral gateways in `communications/`: `PhoneVerificationGateway` (Twilio
Verify-shaped OTP) and `SmsGateway` (Programmable Messaging-shaped), currently backed by
logging fakes that never send anything. Webhook skeletons at `/api/v1/webhooks/sms/inbound`
and `/status` validate a shared-secret signature (`APP_WEBHOOK_SECRET`, fail-closed),
deduplicate by provider message id, and return fast 2xx. `communication_consent` and
`communication_outbox` tables back the future booking-message flow. Production TODOs
tracked: Twilio signature validation, A2P 10DLC registration for US long-code SMS.

## Migrations

Flyway migrations live in `src/main/resources/db/migration` (V1–V10) — see the README there.
